package com.persistentbit.sql.mavenplugin;

import com.persistentbit.core.utils.IO;
import com.persistentbit.sql.connect.SimpleConnectionProvider;
import com.persistentbit.sql.substemagen.DbSubstemaGen;
import com.persistentbit.substema.compiler.SubstemaCompiler;
import com.persistentbit.substema.compiler.values.RSubstema;
import com.persistentbit.substema.dependencies.DependencySupplier;
import com.persistentbit.substema.substemagen.SubstemaSourceGenerator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.List;

/**
 * create a Db substema by importing the data structure from an existing database.
 *
 * @author petermuys
 * @since 5/11/16
 */
@Mojo(
	name = "import-db",
	requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class SqlImportDbMojo extends AbstractSqlMojo{



	@Parameter(required = true)
	String       dbDriverClass;
	@Parameter(required = true)
	String       dbUrl;
	@Parameter
	String       dbUserName;
	@Parameter
	String       dbPassword;
	@Parameter(name = "packages", required = true)
	List<String> packages;

	public void execute()  throws MojoExecutionException, MojoFailureException {
		try{
			getLog().info("Creating database connection");
			SimpleConnectionProvider connectionProvider = new SimpleConnectionProvider(
				dbDriverClass, dbUrl, dbUserName, dbPassword
			);
			SubstemaCompiler compiler = new SubstemaCompiler(createDependencySupplier());
			packages.forEach(p -> {
				getLog().info("Importing db for package " + p);
				RSubstema     baseSubstema = compiler.compile(p);
				DbSubstemaGen gen          = new DbSubstemaGen(connectionProvider, baseSubstema, compiler, null, null);
				gen.loadTables();
				baseSubstema = gen.replaceBase(false);
				SubstemaSourceGenerator codeGen = new SubstemaSourceGenerator();
				codeGen.addSubstema(baseSubstema);
				IO.writeFile(codeGen
								 .toString(), new File(resourcesDirectory, p + DependencySupplier.substemaDefFileExtension));
			});



		}catch (Exception e){
			getLog().error("General error",e);
			throw new MojoFailureException("Error while generating db code",e);
		}

	}
}
