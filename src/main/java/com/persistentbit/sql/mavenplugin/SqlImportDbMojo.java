package com.persistentbit.sql.mavenplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

/**
 * create a Db substema by importing the data structure from an existing database.
 *
 * @author petermuys
 * @since 5/11/16
 */
@Mojo(
	name="generate-db",
	defaultPhase = LifecyclePhase.GENERATE_SOURCES,
	requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class SqlImportDbMojo extends AbstractMojo{

	@Parameter(defaultValue = "src/main/resources",required = true)
	File resourcesDirectory;

	@Parameter(required = true)
	String dbDriverClass;
	@Parameter(required = true)
	String dbUrl;
	@Parameter
	String userName;
	@Parameter
	String password;


}
