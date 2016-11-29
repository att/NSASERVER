/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * At the logical/interface level, this class reads and writes byte arrays to a
 * file, assigning an address to each array.
 * <p>
 * The implementation allocates fixed-size blocks in a random access file. Byte
 * arrays are written to a chain of blocks. Each block has a block type flag and
 * a 4 byte value that is either the address of the next block in this chain or,
 * for the last block in the chain, the length of the data in the block.
 * 
 * @author peter@rathravane.com
 *
 */
public class rrBlockFile
{
	public static final long kBadHandle = -1;

	/**
	 * Initialize a block file with the given block size. If the file exists,
	 * its contents are destroyed.
	 * 
	 * @param file
	 * @param blockSize
	 * @throws IOException
	 */
	public static void initialize ( File file, int blockSize ) throws IOException
	{
		// dumb as it would be, the minimum block size is the overhead + 1 byte
		if ( blockSize < kOffsetToBlockData + 1 )
		{
			throw new IllegalArgumentException ( "The block size is too small." );
		}
		
		final RandomAccessFile f = new RandomAccessFile ( file, "rw" );

		// if the file exists, truncate it. This is required because new blocks are
		// allocated with an index that is the length of the file.
		f.setLength ( 0 );

		// write the header, with block size and a random salt
		f.seek ( 0 );
		f.write ( "rrbf".getBytes ( Charset.forName ( "UTF-8" ) ));
		f.writeInt ( 1 );
		f.writeInt ( 0 );
		f.writeInt ( blockSize );
		f.writeLong ( kBadHandle );

		byte[] salt = SecureRandom.getSeed ( kSaltSize );
		f.write ( salt );

		// done
		f.close ();
	}

	/**
	 * Open an existing block file in read-write mode, and without a password.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public rrBlockFile ( File file ) throws IOException
	{
		this ( file, true );
	}

	/**
	 * Open an existing block file with the given read-write mode, and without
	 * a password.
	 * 
	 * @param file
	 * @param withWrite
	 * @throws IOException
	 */
	public rrBlockFile ( File file, boolean withWrite ) throws IOException
	{
		this ( file, withWrite, null );
	}

	/**
	 * open an existing block file for read or read/write access
	 * @param file
	 * @param withWrite when true, open for writes
	 * @param passwd a password for the file, which can be null
	 * @throws FileNotFoundException
	 */
	public rrBlockFile ( File file, boolean withWrite, String passwd ) throws IOException
	{
		fUnderlyingFile = file;
		fFile = new RandomAccessFile ( file, ( withWrite ? "rw" : "r" ) );
		fCanWrite = withWrite;

		fFile.seek ( 0 );

		byte[] tag = new byte[4];
		fFile.read ( tag );
		String tagString = new String ( tag );
		if ( !tagString.equals ( "rrbf" ) )
		{
			throw new IOException ( "unrecognized file format" );
		}

		fMajor = fFile.readInt ();
		fMinor = fFile.readInt ();
		if ( fMajor != 1 || fMinor != 0 )
		{
			throw new IOException ( "unrecognized file format" );
		}

		fBlockSize = fFile.readInt ();
		fCurrentBlockData = new byte [ fBlockSize ];
		fBlockDataSize = fBlockSize - kOffsetToBlockData;

		fDeleteChain = fFile.readLong ();

		if ( passwd != null )
		{
			// read a salt from the file
			byte[] saltBytes = new byte [ kSaltSize ];
			fFile.read ( saltBytes );
			initKey ( passwd, saltBytes );
		}
	}

	public String getFilePath ()
	{
		return fUnderlyingFile.getAbsolutePath ();
	}
	
	/**
	 * Close the file.
	 * @throws IOException
	 */
	public void close () throws IOException
	{
		fFile.close ();
	}

	/**
	 * Translate a 0-based block index to an address based on the block size used in this
	 * file. Note this is not generally useful, but applications that are careful can construct
	 * data in such as way as to place the data in known locations. For example, the first byte
	 * array (at "indexToAddress(0)") might contain a map to other important byte arrays.
	 * @param index
	 * @return an address value
	 */
	public long indexToAddress ( long index )
	{
		return kHeaderLength + ( index * fBlockSize );
	}

	/**
	 * Add a byte array to this file and return its address.
	 * @param bytes
	 * @return the address for the stored byte array
	 * @throws IOException
	 */
	public long create ( byte[] bytes ) throws IOException
	{
		final ByteArrayInputStream bais = new ByteArrayInputStream ( bytes );
		return create ( bais );
	}

	/**
	 * Add a byte array from an input stream and return its address.
	 * @param is an input stream
	 * @return the address for the stored byte array
	 * @throws IOException
	 */
	public long create ( InputStream is ) throws IOException
	{
		long result = allocateBlock ();
		final OutputStream os = writeStream ( result );
		copyStream ( is, os );
		os.close ();
		return result;
	}

	/**
	 * read a byte array from the file given its address
	 * @param address
	 * @return an array of bytes stored at the given address
	 * @throws IOException
	 */
	public byte[] read ( long address ) throws IOException
	{
		final InputStream in = readToStream ( address );
		final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		copyStream ( in, baos );
		baos.close ();
		return baos.toByteArray ();
	}

	/**
	 * Read a stream to a byte array in the file given its address.
	 * <p>
	 * NOTE: It's critical that no other methods on this object are called until
	 * you're finished reading the stream.
	 * <p>
	 * @param address
	 * @return a stream to read
	 * @throws IOException
	 */
	public InputStream readToStream ( long address ) throws IOException
	{
		InputStream result = new blockReadStream ( address );
		if ( fKey != null )
		{
			result = new CipherInputStream ( result, getCipher ( false ) );
		}
		return result;
	}

	/**
	 * Append the given byte array to the existing byte array at 'address'.
	 * <p>
	 * Note that in password protected files, this operation can take some time,
	 * because the existing byte array must be read, decrypted, appended, and encrypted
	 * before being written back to the file.
	 * <p>
	 * In clear files, the operation seeks to the end of the existing block
	 * chain and appends the new data.
	 * 
	 * @param address
	 * @param bytes
	 * @throws IOException
	 */
	public void append ( long address, byte[] bytes ) throws IOException
	{
		if ( fKey != null )
		{
			final byte[] thereNow = read ( address );
			final OutputStream os = writeStream ( address );
			os.write ( thereNow );
			os.write ( bytes );
			os.close ();
		}
		else
		{
			long lastBlock = getLastBlockInChain ( address );
			final byte[] thereNow = read ( lastBlock );
			final OutputStream os = writeStream ( lastBlock );
			os.write ( thereNow );
			os.write ( bytes );
			os.close ();
		}
	}

	/**
	 * Overwrite the existing byte array at 'address' with the given byte array.
	 * @param address
	 * @param bytes
	 * @throws IOException
	 */
	public void overwrite ( long address, byte[] bytes ) throws IOException
	{
		final ByteArrayInputStream bais = new ByteArrayInputStream ( bytes );
		overwrite ( address, bais );
	}

	/**
	 * Overwrite the existing byte array at 'address' with bytes from the given
	 * input stream.
	 * @param address
	 * @param bytes
	 * @throws IOException
	 */
	public void overwrite ( long address, InputStream bytes ) throws IOException
	{
		final OutputStream os = writeStream ( address );
		copyStream ( bytes, os );
		os.close ();
	}

	/**
	 * Delete the byte array at 'address'.
	 * @param address
	 * @throws IOException
	 */
	public void delete ( long address ) throws IOException
	{
		if ( !fCanWrite )
		{
			throw new IOException ( "opened read-only" );
		}

		// to delete a chain, we chain the current delete chain on to the back
		// of this chain we're deleting, then set the front of the deleting
		// chain in the header

		long current = getLastBlockInChain ( address );
		storeBlock ( current, fDeleteChain, new byte[0] );

		fDeleteChain = address;
		writeDeleteChainPointer ();
	}

	private final File fUnderlyingFile;
	private RandomAccessFile fFile;
	private final boolean fCanWrite;

	// header
	private final int fMajor;
	private final int fMinor;
	private final int fBlockSize;
	private final int fBlockDataSize;

	private long fDeleteChain;

	private boolean fCurrentIsLast;
	private long fCurrentNextOrSize;
	private byte[] fCurrentBlockData;

	private PBEParameterSpec fParamSpec;
	private SecretKey fKey;

	private static final int kSaltSize = 8;
	private static final int kHeaderLength =
		4 +	// "rrbf"
		4 +	// major version
		4 +	// minor version
		4 +	// block size
		8 +	// delete chain
		kSaltSize	// salt for password encrypted file
	;
	private static final int kDeleteChainPointerLocation = 16;
	private static final int kOffsetToBlockData = 8; // for size / next block pointer

	void copyStream ( InputStream is, OutputStream os ) throws IOException
	{
		copyStream ( is, os, fBlockSize );
	}

	static void copyStream ( InputStream is, OutputStream os, int bufferSize ) throws IOException
	{
		final byte[] buffer = new byte [ bufferSize ];
		int len;
		while ( ( len = is.read ( buffer ) ) != -1 )
		{
			os.write ( buffer, 0, len );
		}
	}

	private long allocateBlock () throws IOException
	{
		long result = kBadHandle;
		if ( fDeleteChain != kBadHandle )
		{
			result = fDeleteChain;
			fDeleteChain = getNextBlockFrom ( result );
			writeDeleteChainPointer ();
		}
		else
		{
			result = fFile.length ();

			// an earlier implementation left this out as an optimization.
			// unfortunately, a long chain will allocate the next block
			// before storing the current block. without the write here, the
			// file length never changed, so the same block was reissued over
			// and over.
			storeBlock ( result, new byte[0], 0 );
		}
		return result;
	}

	private void writeDeleteChainPointer () throws IOException
	{
		fFile.seek ( kDeleteChainPointerLocation );
		fFile.writeLong ( fDeleteChain );
	}

	private void loadBlock ( long address ) throws IOException
	{
		long maxAddress = fFile.length ();
		if ( address == maxAddress )
		{
			fCurrentIsLast = true;
			fCurrentNextOrSize = 0;
			fCurrentBlockData = new byte [ 0 ];
		}
		else
		{
			fFile.seek ( address );

			int expect = fBlockDataSize;
			final long sizeData = fFile.readLong ();
			if ( sizeData >= 0 )
			{
				fCurrentIsLast = true;
				fCurrentNextOrSize = sizeData;
				expect = (int) ( fCurrentNextOrSize & 0x0000ffff );
			}
			else
			{
				fCurrentIsLast = false;
				fCurrentNextOrSize = -1 * sizeData;
			}

			fCurrentBlockData = new byte [ expect ];
			if ( expect > fFile.read ( fCurrentBlockData ) )
			{
				throw new IOException ( "block size too small" );
			}
		}
	}

	private OutputStream writeStream ( long address ) throws IOException
	{
		OutputStream result = new blockOutputStream ( address );
		if ( fKey != null )
		{
			result = new CipherOutputStream ( result, getCipher ( true ) );
		}
		return result;
	}

	private void storeBlock ( long thisBlock, long nextBlock, byte[] bytes ) throws IOException
	{
		fFile.seek ( thisBlock );

		if ( nextBlock == kBadHandle )
		{
			fFile.writeLong ( bytes.length );
		}
		else
		{
			fFile.writeLong ( -1 * nextBlock );
		}

		final byte[] block = new byte [ fBlockDataSize ];
		System.arraycopy ( bytes, 0, block, 0, bytes.length );
		fFile.write ( block );
	}

	private void storeBlock ( long thisBlock, byte[] bytes, int size ) throws IOException
	{
		// storing the last block in a chain...
		
		if ( size < 0 )
		{
			throw new IllegalArgumentException ( "Data size in last block may not be less than 0." );
		}

		long nextBlockWas = getNextBlockFrom ( thisBlock );

		fFile.seek ( thisBlock );
		fFile.writeLong ( size );

		final byte[] block = new byte [ fBlockDataSize ];
		System.arraycopy ( bytes, 0, block, 0, size );
		fFile.write ( block );

		if ( nextBlockWas != kBadHandle )
		{
			// the prior byte array continued into another block. that block
			// is no longer needed, so delete it. (this is important in an
			// overwrite case)
			delete ( nextBlockWas );
		}
	}

	private long getLastBlockInChain ( long handle ) throws IOException
	{
		long current = handle;
		long next = getNextBlockFrom ( current );
		while ( next != kBadHandle )
		{
			current = next;
			next = getNextBlockFrom ( current );
		}
		return current;
	}

	private long getNextBlockFrom ( long handle ) throws IOException
	{
		long result = kBadHandle;
		if ( handle != fFile.length () )
		{
			fFile.seek ( handle );
			long nextOrSize = fFile.readLong ();
			if ( nextOrSize < kBadHandle )
			{
				result = nextOrSize * -1;
			}
		}
		return result;
	}

	// RFC 2898 recommends at least 1000 iterations...
	private static final int kPbeIterationCount = 1000;
	private static final int kPbeKeyLength = 8;

	private Cipher getCipher ( boolean toEncrypt ) throws IOException
	{
		if ( fKey == null )
		{
			throw new IOException ( "Attempt to create cipher without key initialization." );
		}
		try
		{
			final Cipher cipher = Cipher.getInstance ( fKey.getAlgorithm () );
			cipher.init ( ( toEncrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE ), fKey, fParamSpec );
			return cipher;
		}
		catch ( GeneralSecurityException e )
		{
			throw new IOException ( e );
		}
	}

	private void initKey ( String password, byte[] salt ) throws IOException
	{
		try
		{
			final PBEKeySpec keySpec = new PBEKeySpec ( password.toCharArray (), salt, kPbeIterationCount, kPbeKeyLength );
			fParamSpec = new PBEParameterSpec ( keySpec.getSalt (), keySpec.getIterationCount () );

			final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance ( "PBE" );
			fKey = keyFactory.generateSecret ( keySpec );
		}
		catch ( NoSuchAlgorithmException e )
		{
			throw new IOException ( e );
		}
		catch ( InvalidKeySpecException e )
		{
			throw new IOException ( e );
		}
	}

	// NOTE: if the currently loaded block changes between calls to read(),
	// this class will return unpredictable results.
	private class blockReadStream extends InputStream
	{
		public blockReadStream ( long addr ) throws IOException
		{
			fCurrReadBlock = addr;
			loadBlock ( fCurrReadBlock );
			fOffset = 0;
		}
	
		@Override
		public int read () throws IOException
		{
			if ( fCurrReadBlock == -1 )
			{
				return -1;
			}
	
			// make sure there's data available
			final int dataInBlock = fCurrentBlockData.length;
			if ( fOffset >= dataInBlock )
			{
				// load next...
				if ( !fCurrentIsLast )
				{
					fCurrReadBlock = fCurrentNextOrSize;
					loadBlock ( fCurrReadBlock );
					fOffset = 0;
				}
				else	// last block
				{
					fCurrReadBlock = kBadHandle;
				}
			}
	
			int result = -1;
			if ( fCurrReadBlock != kBadHandle )
			{
				result = ( 0xff & fCurrentBlockData [ fOffset++ ] );
			}
			return result;
		}
	
		private long fCurrReadBlock;
		private int fOffset;
	}

	private class blockOutputStream extends OutputStream
	{
		public blockOutputStream ( long address ) throws IOException
		{
			if ( !fCanWrite )
			{
				throw new IOException ( "opened read-only" );
			}
			fCurrentBlock = address;
			fBuffer = new byte [ fBlockDataSize ];
			fSize = 0;
		}
	
		@Override
		public void write ( int b ) throws IOException
		{
			if ( b > 127 || b < -128)
			{
				throw new IOException ( "byte value out of range" );
			}

			byte bb = (byte)( b & 0xff );
			if ( fSize < fBlockDataSize )
			{
				fBuffer [ fSize++ ] = bb;
			}
			else
			{
				// buffer full, this byte goes in next block
				long nextBlock = allocateBlock ();
				storeBlock ( fCurrentBlock, nextBlock, fBuffer );
				fCurrentBlock = nextBlock;
				fSize = 1;
				fBuffer[0] = bb;
			}
		}
	
		@Override
		public void close () throws IOException
		{
			storeBlock ( fCurrentBlock, fBuffer, fSize );
		}
	
		private long fCurrentBlock;
		private byte[] fBuffer;
		private int fSize;
	}
}
