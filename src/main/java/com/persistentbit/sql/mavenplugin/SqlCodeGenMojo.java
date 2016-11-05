package com.persistentbit.sql.mavenplugin;

import com.persistentbit.core.collections.PList;
import com.persistentbit.core.collections.PStream;
import com.persistentbit.sql.staticsql.codegen.DbJavaGen;
import com.persistentbit.substema.compiler.SubstemaCompiler;
import com.persistentbit.substema.dependencies.DependencySupplier;
import com.persistentbit.substema.javagen.GeneratedJava;
import com.persistentbit.substema.javagen.JavaGenOptions;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Generate Sql db java classes from a substema file.
 **/
@Mojo(
	name = "generate-db",
	defaultPhase = LifecyclePhase.GENERATE_SOURCES,
	requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class SqlCodeGenMojo extends AbstractSqlMojo{


	@Parameter(defaultValue = "target/generated-sources/db", required = true)
	File outputDirectory;


	@Parameter(name = "packages", required = true)
	List<String> packages;


	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			getLog().info("Compiling DB...");

			DependencySupplier dependencySupplier = createDependencySupplier();
			SubstemaCompiler   compiler           = new SubstemaCompiler(dependencySupplier);
			//PList<RSubstema> substemas = PList.from(packages).map(p -> compiler.compile(p));

			//substemas.forEach(ss -> getLog().info(ss.toString()));

			if(!outputDirectory.exists()) {
				if(outputDirectory.mkdirs() == false) {
					throw new MojoExecutionException("Can't create output folder " + outputDirectory.getAbsolutePath());
				}
			}
			project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
			JavaGenOptions genOptions = new JavaGenOptions(true, true);


			PStream.from(packages).forEach(packageName -> {
				//SubstemaJavaGen.generateAndWriteToFiles(compiler,genOptions,ss,outputDirectory);
				PList<GeneratedJava> genCodeList = DbJavaGen.generate(genOptions, packageName, compiler);

				genCodeList.forEach(g -> {
					String packagePath = g.name.getPackageName().replace('.', File.separatorChar);
					File   dest        = new File(outputDirectory, packagePath);
					if(dest.exists() == false) { dest.mkdirs(); }
					dest = new File(dest, g.name.getClassName() + ".java");
					getLog().info("Generating " + dest.getAbsolutePath());
					try(FileWriter fw = new FileWriter(dest)) {
						fw.write(g.code);
					} catch(IOException io) {
						getLog().error(io);
						throw new RuntimeException("Can't write to " + dest.getAbsolutePath());
					}
				});
			});


		} catch(Exception e) {
			getLog().error("General error", e);
			throw new MojoFailureException("Error while generating db code", e);
		}


	}


}
