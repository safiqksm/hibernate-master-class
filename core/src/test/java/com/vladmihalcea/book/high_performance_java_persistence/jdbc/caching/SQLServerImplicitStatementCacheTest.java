package com.vladmihalcea.book.high_performance_java_persistence.jdbc.caching;

import com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.providers.TaskEntityProvider;
import com.vladmihalcea.hibernate.masterclass.laboratory.util.AbstractSQLServerIntegrationTest;
import oracle.jdbc.OracleConnection;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * SQLServerImplicitStatementCacheTest - Test SQL Server implicit Statement cache
 *
 * @author Vlad Mihalcea
 */
public class SQLServerImplicitStatementCacheTest extends AbstractSQLServerIntegrationTest {

    public static final String INSERT_TASK = "insert into task (id, status) values (?, ?)";

    private TaskEntityProvider entityProvider = new TaskEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    public void init() {
        super.init();
        doInConnection(connection -> {
            try (
                    PreparedStatement postStatement = connection.prepareStatement(INSERT_TASK);
            ) {
                int postCount = getPostCount();

                int index;

                for (int i = 0; i < postCount; i++) {
                    index = 0;
                    TaskEntityProvider.StatusType statusType;
                    if(i > postCount * 0.99) {
                        statusType = TaskEntityProvider.StatusType.FAILED;
                    } else if(i > postCount * 0.95) {
                        statusType = TaskEntityProvider.StatusType.TO_D0;
                    } else {
                        statusType = TaskEntityProvider.StatusType.DONE;
                    }
                    postStatement.setInt(++index, i);
                    postStatement.setString(++index, statusType.name());
                    postStatement.executeUpdate();
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void testStatementCaching() {
        doInConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                    "select * from task where status = ? OPTION(RECOMPILE)"
                )) {
                    statement.setString(1, TaskEntityProvider.StatusType.FAILED.name());
                    statement.execute();
                }
        });
    }

    protected int getPostCount() {
        return 1000;
    }

    @Override
    protected boolean proxyDataSource() {
        return false;
    }
}
