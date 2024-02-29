/**
 * File:     OCRDDockerProcessorServiceProvider.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     15.05.2023
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker;

import java.io.IOException;
import java.util.List;

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
public abstract class OCRDDockerProcessorServiceProvider extends CoreProcessorServiceProvider {
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

		if (dockerProcess.isStopContainerProcessSet()) {
			try {
				dockerProcess.getStopContainerProcess().execute(dockerProcess.getStopContainerProcessArguments());
			} catch (IOException e) {
				if (dockerProcess.isProcessSet())
					dockerProcess.getProcess().cancel();

			}
		} else if (dockerProcess.isProcessSet())
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
		 * The system process to stop the container.
		 */
		private SystemProcess stopContainerProcess = null;

		/**
		 * The arguments for the system process to stop the container.
		 */
		private List<String> stopContainerProcessArguments = null;

		/**
		 * Configure the docker process.
		 * 
		 * @param process                       The process.
		 * @param stopContainerProcess          The system process to stop the
		 *                                      container.
		 * @param stopContainerProcessArguments The arguments for the system process to
		 *                                      stop the container.
		 * @since 1.8
		 */
		public void configure(SystemProcess process, SystemProcess stopContainerProcess,
				List<String> stopContainerProcessArguments) {
			this.process = process;

			this.stopContainerProcess = stopContainerProcess;
			this.stopContainerProcessArguments = this.stopContainerProcess == null ? null
					: stopContainerProcessArguments;
		}

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
		 * Returns true if the system process to stop the container is set.
		 *
		 * @return True if the system process to stop the container is set.
		 * @since 1.8
		 */
		public boolean isStopContainerProcessSet() {
			return stopContainerProcess != null;
		}

		/**
		 * Returns the system process to stop the container.
		 *
		 * @return The system process to stop the container.
		 * @since 1.8
		 */
		public SystemProcess getStopContainerProcess() {
			return stopContainerProcess;
		}

		/**
		 * Returns the arguments for the system process to stop the container.
		 *
		 * @return The arguments for the system process to stop the container.
		 * @since 1.8
		 */
		public List<String> getStopContainerProcessArguments() {
			return stopContainerProcessArguments;
		}

	}
}
