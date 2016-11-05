package com.persistentbit.sql.mavenplugin;

import com.persistentbit.core.collections.PList;
import com.persistentbit.core.collections.PStream;
import com.persistentbit.substema.dependencies.DependencySupplier;
import com.persistentbit.substema.dependencies.SupplierDef;
import com.persistentbit.substema.dependencies.SupplierType;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

/**
 * Base class for sql mojo's
 *
 * @author petermuys
 * @since 5/11/16
 */
public abstract class AbstractSqlMojo extends AbstractMojo{


	@Parameter(property = "project", required = true, readonly = true)
	MavenProject project;

	@Parameter(defaultValue = "src/main/resources", required = true)
	File resourcesDirectory;

	protected DependencySupplier createDependencySupplier() throws MojoExecutionException {

		getLog().info("Compiling Substemas...");
		PList<SupplierDef> supplierDefs = PList.empty();
		try {
			if(resourcesDirectory.exists()) {
				getLog().info("Adding Dependency Supplier " + SupplierType.folder + " , " + resourcesDirectory
					.getAbsolutePath());
				supplierDefs =
					supplierDefs.plus(new SupplierDef(SupplierType.folder, resourcesDirectory.getAbsolutePath()));

			}
			List<String> classPathElements = project.getCompileClasspathElements();
			if(classPathElements != null) {
				supplierDefs = supplierDefs.plusAll(PStream.from(classPathElements).map(s -> {
					File f = new File(s);
					if(f.exists()) {
						SupplierType type = f.isDirectory() ? SupplierType.folder : SupplierType.archive;
						getLog().info("Adding Dependency Supplier " + type + " , " + f.getAbsolutePath());
						return new SupplierDef(type, f.getAbsolutePath());
					}
					else {
						return null;
					}
				}).filterNulls());
			}

		} catch(Exception e) {
			throw new MojoExecutionException("Error building dependencyList", e);
		}

		return new DependencySupplier(supplierDefs);
	}

}
