/**
 * File:     OCRDProcessorServiceProvider.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     15.05.2023
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi;

import de.uniwuerzburg.zpd.ocr4all.application.spi.core.CoreProcessorServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.util.SystemProcess;

/**
 * Defines core processors for service providers for OCRD. When implementing the
 * required method {@link ProcessServiceProvider.Processor#execute}, this class
 * should be initialized by calling the method {@link #initialize} at the
 * beginning and completed by calling the method {@link #complete}.
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 1.8
 */
public abstract class OCRDProcessorServiceProvider extends CoreProcessorServiceProvider {
	/**
	 * The docker process.
	 */
	protected final DockerProcess dockerProcess = new DockerProcess();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.CoreProcessorServiceProvider
	 * #cancel()
	 */
	@Override
	public void cancel() {
		super.cancel();

		if (dockerProcess.isProcessSet())
			dockerProcess.getProcess().cancel();
	}

	/**
	 * Defines docker processes.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	protected class DockerProcess {
		/**
		 * The process.
		 */
		private SystemProcess process = null;

		/**
		 * Returns true if the process is set.
		 *
		 * @return True if the process is set.
		 * @since 1.8
		 */
		public boolean isProcessSet() {
			return process != null;
		}

		/**
		 * Returns the process.
		 *
		 * @return The process.
		 * @since 1.8
		 */
		public SystemProcess getProcess() {
			return process;
		}

		/**
		 * Set the process.
		 *
		 * @param process The process to set.
		 * @since 1.8
		 */
		public void setProcess(SystemProcess process) {
			this.process = process;
		}

	}
}
