/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.db;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.att.nsa.security.NsaApiKey;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;

@Deprecated
public class CassandraAuthDb<K extends NsaApiKey> implements NsaAuthDb<K> {
	
	private final Cluster cluster;
	private final Session session;
	private final ConcurrentHashMap<StatementName, PreparedStatement> preparedStatements;
	private final Object prepareStatementCreateLock;
	private final List<InetAddress> contactPoints;
	private final int port;
	
	private enum StatementName {
		CREATE_RESOURCE,
		GET_ACL,
		PERMIT,
		DENY
	}
	
	@SuppressWarnings("unused") //Hide the implicit constructor
	private CassandraAuthDb() {
		this.cluster = null;
		this.session = null;
		this.preparedStatements = null;
		this.prepareStatementCreateLock = null;
		this.port = -1;
		this.contactPoints = null;
	}
	
	public CassandraAuthDb(List<String> contactPoints, int port) {
		
		this.contactPoints = new ArrayList<InetAddress> (contactPoints.size());
		
		for (String contactPoint : contactPoints) {
			try {
				this.contactPoints.add(InetAddress.getByName(contactPoint));
			} catch (UnknownHostException e) {
                throw new IllegalArgumentException(e.getMessage());
			}
		}
		
		this.port = port;
		
		cluster = (new Cluster.Builder()).withPort (this.port)
				.addContactPoints(this.contactPoints)
				.withSocketOptions(new SocketOptions().setReadTimeoutMillis(60000).setKeepAlive(true).setReuseAddress(true))
				.withLoadBalancingPolicy(new RoundRobinPolicy())
				.withReconnectionPolicy(new ConstantReconnectionPolicy(500L))
				.withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.ONE))
				.build ();
		
		session = cluster.newSession();
		preparedStatements = new ConcurrentHashMap<StatementName, PreparedStatement> ();
		prepareStatementCreateLock = new Object();
	}
	
	private void createKeyspaceIfNotExists() {
		session.execute("CREATE KEYSPACE IF NOT EXISTS fe3c WITH replication = {'class':'SimpleStrategy', 'replication_factor': '1'}");
	}
	
	private void createTableIfNotExists() {
		session.execute("CREATE TABLE IF NOT EXISTS fe3c.authorizations (resource text, role text, user text, PRIMARY KEY(resource, role, user));");
	}
	
	private void prepareStatements() {
		
		createKeyspaceIfNotExists();
		createTableIfNotExists();
		
		preparedStatements.put(StatementName.CREATE_RESOURCE, session.prepare("INSERT INTO fe3c.authorizations (resource, role, user) VALUES (?, 'OWNER', ?) IF NOT EXISTS"));
		preparedStatements.put(StatementName.GET_ACL, session.prepare("SELECT * FROM fe3c.authorizations WHERE resource = ?"));
		preparedStatements.put(StatementName.DENY, session.prepare("DELETE FROM fe3c.authorizations WHERE resource = ? AND role = 'USER' AND user = ?"));
		preparedStatements.put(StatementName.PERMIT, session.prepare("INSERT INTO fe3c.authorizations (resource, role, user) VALUES(?, 'USER', ?)"));
	}
	
	private PreparedStatement getStatement(StatementName name) {
		if (preparedStatements.isEmpty()) {
			synchronized (prepareStatementCreateLock) {
				if (preparedStatements.isEmpty()) {
					prepareStatements();
				}
			}
		}
		
		return preparedStatements.get(name);
	}

	@Override
	public boolean isAuthorized(K key, String resource, String operation)
			throws AuthorizationServiceUnavailableException {
		final BoundStatement permitStatement = new BoundStatement(getStatement(StatementName.GET_ACL));
		permitStatement.bind(resource);
		
		final ResultSet results = session.execute(permitStatement);
		
		final String comparisonKey = (key == null) ? "" : key.getKey();
		
		for (Row result : results) {
			if (result.getString("user").equals(comparisonKey) || result.getString("user").equals("*")) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public void createResource(K owner, String resource) throws AuthorizationServiceUnavailableException {
		final BoundStatement createStatement = new BoundStatement(getStatement(StatementName.CREATE_RESOURCE));
		
		createStatement.bind(resource, owner.getKey());
		
		session.execute(createStatement);
	}
	
	@Override
	public void permit(K key, String resource) {
		final BoundStatement permitStatement = new BoundStatement(getStatement(StatementName.PERMIT));
		
		permitStatement.bind(resource, (key == null) ? "" : key.getKey());
		
		session.execute(permitStatement);
	}

	@Override
	public void deny(K key, String resource) {
		final BoundStatement denyStatement = new BoundStatement(getStatement(StatementName.DENY));
		
		denyStatement.bind(resource, (key == null) ? "" : key.getKey());
		
		session.execute(denyStatement);
	}

	@Override
	public void permitAll(String resource) {
		final BoundStatement permitStatement = new BoundStatement(getStatement(StatementName.PERMIT));
		
		permitStatement.bind(resource, "*");
		
		session.execute(permitStatement);
	}

	@Override
	public void denyAll(String resource) {
		final BoundStatement denyStatement = new BoundStatement(getStatement(StatementName.DENY));
		
		denyStatement.bind(resource, "*");
		
		session.execute(denyStatement);
	}

}
