package com.persistentbit.sql.mavenplugin;

import com.persistentbit.core.collections.PList;
import com.persistentbit.core.collections.PStream;
import com.persistentbit.core.logging.AnsiColor;
import com.persistentbit.core.logging.LogPrinter;
import com.persistentbit.core.result.Result;
import com.persistentbit.core.utils.IndentOutputStream;
import com.persistentbit.core.utils.IndentPrintStream;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;
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
				PList<Result<GeneratedJava>> genCodeList = DbJavaGen.generate(genOptions, packageName, compiler);
				genCodeList.forEach(resultGen -> {
					{
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						LogPrinter lp = IndentOutputStream.of(bout)
							.flatMap(os -> IndentPrintStream.of(os, Charset.forName("UTF-8")))
							.map(s -> new LogPrinter(new AnsiColor(true), s))
							.orElseThrow();
						lp.print(resultGen);
						getLog().error("Generated result: " + bout.toString());
					}
					Result<File> resultFile = resultGen.flatMap(rg -> rg.writeToFile(outputDirectory));
					resultFile.ifFailure(failure -> getLog().error(failure.getException()));
					resultFile.ifPresent(success -> getLog().info("Generated " + success.getValue().getAbsolutePath()));

				});
/*
				genCodeList.forEach(gr -> {
					GeneratedJava g = gr.orElseThrow();
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
				});*/
			});


		} catch(Exception e) {
			getLog().error("General error", e);
			throw new MojoFailureException("Error while generating db code", e);
		}


	}


}
