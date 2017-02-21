/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.storage.backing.sqlprovider;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import me.lucko.luckperms.common.storage.DatastoreConfiguration;

import java.sql.SQLException;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;

public class MSSQLProvider extends SQLProvider {

    private final DatastoreConfiguration configuration;
    private HikariDataSource hikari;

    public MSSQLProvider(DatastoreConfiguration configuration) {
        super("MSSQL");
        this.configuration = configuration;
    }

    @Override
    public void init() throws Exception {
        HikariConfig config = new HikariConfig();

        String address = configuration.getAddress();
        String[] addressSplit = address.split(":");
        address = addressSplit[0];
        String port = addressSplit.length > 1 ? addressSplit[1] : "1433";

        String database = configuration.getDatabase();
        String username = configuration.getUsername();
        String password = configuration.getPassword();
        
        String url = "jdbc:sqlserver://" + address + ":" + port + ";databaseName=" + database + ";user=" + username + ";password={" + password + "};";

        //config.setMaximumPoolSize(configuration.getPoolSize());
        //config.setPoolName("luckperms");
        //config.setDataSourceClassName("com.microsoft.sqlserver.jdbc.SQLServerDataSource");
        //config.addDataSourceProperty("serverName", address);
        //config.addDataSourceProperty("port", port);
        //config.addDataSourceProperty("databaseName", database);
        //config.addDataSourceProperty("user", username);
        //config.addDataSourceProperty("password", password);
        //config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10)); // 10000
        //config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(5)); // 5000
        //config.setValidationTimeout(TimeUnit.SECONDS.toMillis(3)); // 3000
        //config.setInitializationFailFast(true);
        //config.setConnectionTestQuery("/* LuckPerms ping */ SELECT 1");

        hikari = new HikariDataSource();
        hikari.setMaximumPoolSize(configuration.getPoolSize());
        hikari.setDataSourceClassName("com.microsoft.sqlserver.jdbc.SQLServerDataSource");
        hikari.addDataSourceProperty("URL", url);
        hikari.addDataSourceProperty("user", username);
        hikari.addDataSourceProperty("password", password);
        hikari.setInitializationFailFast(true);
        hikari.setPoolName("luckperms");
    }

    @Override
    public void shutdown() throws Exception {
        if (hikari != null) {
            hikari.close();
        }
    }

    @Override
    public WrappedConnection getConnection() throws SQLException {
    	Connection conn = hikari.getConnection();
        return new WrappedConnection(conn, true);
    }
}
