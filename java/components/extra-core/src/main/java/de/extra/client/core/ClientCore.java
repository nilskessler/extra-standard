/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package de.extra.client.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.oxm.XmlMappingException;

import de.drv.dsrv.extra.marshaller.IExtraMarschaller;
import de.drv.dsrv.extrastandard.namespace.request.RequestTransport;
import de.drv.dsrv.extrastandard.namespace.response.ResponseTransport;
import de.extra.client.core.builder.IExtraRequestBuilder;
import de.extra.client.core.locator.IPluginsLocatorManager;
import de.extra.client.core.observer.OpLogger;
import de.extra.client.core.process.IRequestIdAcquisitionStrategy;
import de.extrastandard.api.exception.ExceptionCode;
import de.extrastandard.api.exception.ExtraConfigRuntimeException;
import de.extrastandard.api.exception.ExtraCoreRuntimeException;
import de.extrastandard.api.exception.ExtraRuntimeException;
import de.extrastandard.api.model.content.IExtraProfileConfiguration;
import de.extrastandard.api.model.content.IInputDataContainer;
import de.extrastandard.api.model.content.IResponseData;
import de.extrastandard.api.model.content.ISingleInputData;
import de.extrastandard.api.model.execution.ICommunicationProtocol;
import de.extrastandard.api.model.execution.IExecution;
import de.extrastandard.api.model.execution.IExecutionPersistence;
import de.extrastandard.api.model.execution.PersistentStatus;
import de.extrastandard.api.model.execution.PhaseQualifier;
import de.extrastandard.api.plugin.IConfigPlugin;
import de.extrastandard.api.plugin.IDataPlugin;
import de.extrastandard.api.plugin.IOutputPlugin;
import de.extrastandard.api.plugin.IResponseProcessPlugin;

@Named("clientCore")
public class ClientCore implements ApplicationContextAware {

	private static final Logger logger = LoggerFactory.getLogger(ClientCore.class);
	// TODO globaler Logger: auslagern
	private static final Logger operation_logger = LoggerFactory.getLogger("de.extra.client.operation");
	private static final Logger message_request_logger = LoggerFactory.getLogger("de.extra.client.message.request");

	ApplicationContext applicationContext;

	@Value("${core.execution.procedure}")
	private String executionProcedure;

	@Value("${core.execution.phase}")
	private String executionPhase;

	@Value("${core.outgoing.validation}")
	private boolean outgoingXmlValidation;

	@Inject
	@Named("pluginsLocatorManager")
	private IPluginsLocatorManager pluginsLocatorManager;

	@Inject
	@Named("extraRequestBuilder")
	IExtraRequestBuilder extraMessageBuilder;

	@Inject
	@Named("extraMarschaller")
	private IExtraMarschaller marshaller;

	@Inject
	@Named("simpleRequestIdAcquisitionStrategy")
	private IRequestIdAcquisitionStrategy requestIdAcquisitionStrategy;

	private IDataPlugin dataPlugin;

	private IConfigPlugin configPlugin;

	private IOutputPlugin outputPlugin;

	private IResponseProcessPlugin responsePlugin;

	private IExecutionPersistence executionPersistence;

	@PostConstruct
	public void init() {
		logger.debug("Plugins initialisieren");
		dataPlugin = pluginsLocatorManager.getConfiguredDataPlugin();

		configPlugin = pluginsLocatorManager.getConfiguredConfigPlugin();

		outputPlugin = pluginsLocatorManager.getConfiguredOutputPlugin();

		responsePlugin = pluginsLocatorManager.getConfiguredResponsePlugin();

		executionPersistence = pluginsLocatorManager.getConfiguredExecutionPesistence();
		logger.debug(OpLogger.LOG_TRENNZEILE);
	}

	/**
	 * <pre>
	 *  1. Holt über den DataPlugin die zu verarbeitenden Daten.
	 *  2. Verpackt die Daten in eine Extra RequestMessage Anhand der Meassagekonfiguration
	 *  3. Versendet die Daten via Outputplugin
	 *  4. Verarbeitet die eXTra Response via Reponse-Process Plugins
	 * </pre>
	 */
	public ClientProcessResult process(final String processParameters) {

		logger.info("Start Process for Procedure {} und Phase {}: ", executionProcedure, executionPhase);

		final ClientProcessResult totalClientProcessResult = applicationContext.getBean("clientProcessResult",
				ClientProcessResult.class);
		if (dataPlugin.isEmpty()) {
			if (dataPlugin.hasWarnings()) {
				totalClientProcessResult.setHasWarnings(true);
			}
			return totalClientProcessResult;
			// Return empty Result
		}

		while (dataPlugin.hasMoreData()) {
			final IInputDataContainer versandDaten = dataPlugin.getData();

			logger.debug(OpLogger.LOG_TRENNZEILE);
			logger.info("Process Daten: " + versandDaten);
			final ClientProcessResult singleProcessResult = processInputData(processParameters, versandDaten);
			totalClientProcessResult.addResult(singleProcessResult);
		}

		return totalClientProcessResult;
	}

	/**
	 * Liefert konfigurierte Phase.
	 *
	 * @return Phase
	 */
	public PhaseQualifier getExecutionPhaseQualifier() {
		return PhaseQualifier.resolveByName(executionPhase);
	}

	/**
	 * @param processParameters
	 * @param phaseQualifier
	 * @param versandDaten
	 * @return
	 */
	private ClientProcessResult processInputData(final String processParameters,
			final IInputDataContainer versandDaten) {

		final IExecution execution = executionPersistence.startExecution(executionProcedure, processParameters,
				getExecutionPhaseQualifier());

		final IExtraProfileConfiguration configFile = configPlugin.getConfigFile();

		final ClientProcessResult clientProcessResult = applicationContext.getBean("clientProcessResult",
				ClientProcessResult.class);
		try {

			for (final ISingleInputData singleContentInputData : versandDaten.getContent()) {
				final ICommunicationProtocol inputData = execution.startInputData(singleContentInputData);

				requestIdAcquisitionStrategy.setRequestId(inputData, singleContentInputData);
			}
			requestIdAcquisitionStrategy.setRequestId(versandDaten, execution);

			final IResponseData responseData = processInputData(versandDaten, configFile, execution);
			clientProcessResult.addResult(versandDaten, responseData);

			execution.endExecution(responseData);

		} catch (final ExtraConfigRuntimeException extraConfigException) {
			logger.error("Exception in der Extra-Processing", extraConfigException);
			clientProcessResult.addException(extraConfigException);
			failed(execution, extraConfigException);
		} catch (final ExtraRuntimeException extraRuntimeException) {
			logger.error("Exception in der Extra-Processing", extraRuntimeException);
			clientProcessResult.addException(extraRuntimeException);
			failed(execution, extraRuntimeException);
		} catch (final Exception exception) {
			logger.error("Exception in der Extra-Processing", exception);
			clientProcessResult.addException(exception);
			failed(execution, exception);
		}
		return clientProcessResult;

	}

	private void failed(final IExecution execution, final ExtraRuntimeException extraRuntimeException) {
		if (execution != null) {
			execution.failed(extraRuntimeException);
		}

	}

	private void failed(final IExecution execution, final Exception exception) {
		if (execution != null) {
			execution.failed(ExceptionCode.UNEXPECTED_INTERNAL_EXCEPTION.name(), exception.getMessage());
		}
	}

	private IResponseData processInputData(final IInputDataContainer inputDataContainer,
			final IExtraProfileConfiguration configFile, final IExecution execution) {
		try {
			final RequestTransport requestTransport = extraMessageBuilder.buildExtraRequestMessage(inputDataContainer,
					configFile);
			execution.updateProgress(PersistentStatus.ENVELOPED);

			if (outgoingXmlValidation) {
				final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				final StreamResult streamResult = new StreamResult(outputStream);
				marshaller.marshal(requestTransport, streamResult, outgoingXmlValidation);
				logger.info("OutgoingXml: {}", outputStream.toString());
				message_request_logger.debug(outputStream.toString());
			}

			final ResponseTransport responseTransport = outputPlugin.outputData(requestTransport);
			execution.updateProgress(PersistentStatus.TRANSMITTED);
			operation_logger.info("eXTra.-Client Request verschickt");
			final IResponseData responseData = responsePlugin.processResponse(responseTransport);

			return responseData;

		} catch (final XmlMappingException xmlMappingException) {
			throw new ExtraCoreRuntimeException(xmlMappingException);
		} catch (final IOException ioException) {
			throw new ExtraCoreRuntimeException(ioException);
		}

	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;

	}

	// (14.12.12) Externer Aufruf
	public boolean changeCommunicationProtocolStatusByOutputIdentifier(final String outputIdentifier,
			final PersistentStatus persistentStatus) {
		final boolean success = executionPersistence
				.changeCommunicationProtocolStatusByOutputIdentifier(outputIdentifier, persistentStatus);
		return success;
	}

}
