/**
 * File:     OCRDMsaProcessorServiceProvider.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     01.03.2024
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa;

import de.uniwuerzburg.zpd.ocr4all.application.spi.core.CoreProcessorServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider;

/**
 * Defines core processors for microservice architecture (MSA) service providers
 * for OCRD. When implementing the required method
 * {@link ProcessServiceProvider.Processor#execute}, this class should be
 * initialized by calling the method
 * {@link CoreProcessorServiceProvider#initialize} at the beginning and
 * completed by calling the method
 * {@link CoreProcessorServiceProvider#complete}.
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 17
 */
public abstract class OCRDMsaProcessorServiceProvider extends CoreProcessorServiceProvider {

}
