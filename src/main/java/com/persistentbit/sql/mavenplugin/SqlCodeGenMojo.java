package com.persistentbit.sql.mavenplugin;

import com.persistentbit.core.collections.PList;
import com.persistentbit.core.collections.PStream;
import com.persistentbit.sql.codegen.DbJavaGen;
import com.persistentbit.substema.dependencies.DependencySupplier;
import com.persistentbit.substema.dependencies.SupplierDef;
import com.persistentbit.substema.dependencies.SupplierType;
import com.persistentbit.substema.javagen.GeneratedJava;
import com.persistentbit.substema.javagen.JavaGenOptions;
import com.persistentbit.substema.javagen.ServiceJavaGen;
import com.persistentbit.substema.compiler.SubstemaCompiler;
import com.persistentbit.substema.compiler.values.RSubstema;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/*
 * Generate packages from a ROD file
 *
 * @goal generate-db
 * @phase generate-db
 *
 * @description Generate db java files
 */
@Mojo(
        name="generate-db",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class SqlCodeGenMojo extends AbstractMojo {
    /*
     * @parameter property="project"
     * @required
     * @readonly
     * @since 1.0
     */
    @Parameter(property = "project",required = true, readonly = true)
    MavenProject project;



    /*
     * @parameter default-value="target/generated-packages/rod"
     * @required
     */
    @Parameter(defaultValue = "target/generated-sources/db",required = true)
    File outputDirectory;

    @Parameter(defaultValue = "src/main/resources",required = true)
    File resourcesDirectory;

    /*
     * Sources
     *
     * @parameter
     * @required
     */
    @Parameter(name="packages",required = true)
    List<String> packages;


    public void execute()  throws MojoExecutionException, MojoFailureException {
        try{
            getLog().info("Compiling DB...");
            PList<SupplierDef> supplierDefs = PList.empty();
            try{
                if(resourcesDirectory.exists()){
                    getLog().info("Adding Dependency Supplier " + SupplierType.folder + " , " + resourcesDirectory.getAbsolutePath());
                    supplierDefs = supplierDefs.plus(new SupplierDef(SupplierType.folder,resourcesDirectory.getAbsolutePath()));

                }
                List<String> classPathElements = project.getCompileClasspathElements();
                if(classPathElements != null){
                    supplierDefs = supplierDefs.plusAll(PStream.from(classPathElements).map(s -> {
                        File f = new File(s);
                        if(f.exists()){
                            SupplierType type = f.isDirectory() ? SupplierType.folder : SupplierType.archive;
                            getLog().info("Adding Dependency Supplier " + type + " , " + f.getAbsolutePath());
                            return new SupplierDef(type,f.getAbsolutePath());
                        } else {
                            return null;
                        }
                    }).filterNulls());
                }

            }catch(Exception e){
                throw new MojoExecutionException("Error building dependencyList",e);
            }
            DependencySupplier dependencySupplier = new DependencySupplier(supplierDefs);
            PList<RSubstema> substemas = SubstemaCompiler.compile(dependencySupplier,PList.from(packages));

            substemas.forEach(ss -> getLog().info(ss.toString()));




            if ( !outputDirectory.exists() ){
                if(outputDirectory.mkdirs() == false){
                    throw new MojoExecutionException("Can't create output folder " + outputDirectory.getAbsolutePath());
                }
            }
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
            JavaGenOptions genOptions  =   new JavaGenOptions(true,true);
            //substemas.forEach(ss -> {
            //    getLog().info("-------- " + ss.getPackageName() + " --------");
            //    getLog().info(JJPrinter.print(true,new JJMapper().write(ss)));
            //});
            substemas.forEach( ss -> {
                PList<GeneratedJava> genCodeList = ServiceJavaGen.generate(genOptions,ss.getPackageName(),ss);
                genCodeList = genCodeList.plusAll(DbJavaGen.generate(genOptions,ss.getPackageName(),ss));

                genCodeList.forEach(g -> {
                    String packagePath = g.name.getPackageName().replace('.',File.separatorChar);
                    File dest = new File(outputDirectory,packagePath);
                    if(dest.exists() == false){ dest.mkdirs(); }
                    dest = new File(dest,g.name.getClassName() + ".java");
                    getLog().info("Generating " + dest.getAbsolutePath());
                    try(FileWriter fw = new FileWriter(dest)){
                        fw.write(g.code);
                    }catch (IOException io){
                        getLog().error(io);
                        throw new RuntimeException("Can't write to " + dest.getAbsolutePath());
                    }
                });
            });


        }catch(MojoExecutionException e){
              throw e;
        }catch (Exception e){
            getLog().error("General error",e);

        }


    }
}
