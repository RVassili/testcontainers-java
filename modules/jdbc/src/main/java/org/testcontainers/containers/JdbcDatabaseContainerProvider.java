package org.testcontainers.containers;

import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.jdbc.ConnectionUrl;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;

/**
 * Base class for classes that can provide a JDBC container.
 */
@Slf4j
public abstract class JdbcDatabaseContainerProvider {

    /**
     * Tests if the specified database type is supported by this Container Provider. It should match to the base image name.
     * @param databaseType {@link String}
     * @return <code>true</code> when provider can handle this database type, else <code>false</code>.
     */
    public abstract boolean supports(String databaseType);

    /**
     * Instantiate a new {@link JdbcDatabaseContainer} without any specified image tag. Subclasses <i>should</i>
     * override this method if possible, to provide a default tag that is more stable than <code>latest</code>`.
     *
     * @return Instance of {@link JdbcDatabaseContainer}
     */
    public JdbcDatabaseContainer newInstance() {
        log.warn("No explicit version tag was provided in JDBC URL and this class ({}) does not " +
            "override newInstance() to set a default tag. `latest` will be used but results may " +
            "be unreliable!", this.getClass().getCanonicalName());
        return this.newInstance("latest");
    }

    public abstract JdbcDatabaseContainer newInstance(DockerImageName dockerImageName);

    /**
     * Instantiate a new {@link JdbcDatabaseContainer} with specified image tag.
     * @param tag
     * @return Instance of {@link JdbcDatabaseContainer}
     */
    public abstract JdbcDatabaseContainer newInstance(String tag);

    /**
     * Instantiate a new {@link JdbcDatabaseContainer} using information provided with {@link ConnectionUrl}.
     * @param url {@link ConnectionUrl}
     * @return Instance of {@link JdbcDatabaseContainer}
     */
    public JdbcDatabaseContainer newInstance(ConnectionUrl url) {
        return basicInstance(url);
    }

    protected JdbcDatabaseContainer newInstanceFromConnectionUrl(ConnectionUrl connectionUrl, final String userParamName, final String pwdParamName) {
        final String databaseName = connectionUrl.getDatabaseName().orElse("test");
        final String user = connectionUrl.getQueryParameters().getOrDefault(userParamName, "test");
        final String password = connectionUrl.getQueryParameters().getOrDefault(pwdParamName, "test");

        final JdbcDatabaseContainer<?> instance = basicInstance(connectionUrl);

        return instance
            .withDatabaseName(databaseName)
            .withUsername(user)
            .withPassword(password);
    }

    private JdbcDatabaseContainer basicInstance(ConnectionUrl url) {
        Objects.requireNonNull(url, "Connection URL cannot be null");

        final JdbcDatabaseContainer result;
        if (url.getRegistry().isPresent()) {
            StringBuilder remoteUrlBuilder = new StringBuilder()
                .append(url.getRegistry().get())
                .append("/")
                .append(url.getImageName());
            if (url.getImageTag().isPresent()) {
                remoteUrlBuilder.append(":")
                    .append(url.getImageTag().get());
            }
            result = newInstance(DockerImageName.parse(remoteUrlBuilder.toString()));
        }
        else if (url.getImageTag().isPresent()) {
            result = newInstance(url.getImageTag().get());
        } else {
            result = newInstance();
        }
        result.withReuse(url.isReusable());
        return result;
    }
}
