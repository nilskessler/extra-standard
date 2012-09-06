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
package de.extra.client.core.plugin.dummies;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.Marshaller;

import de.drv.dsrv.extrastandard.namespace.components.Base64CharSequenceType;
import de.drv.dsrv.extrastandard.namespace.components.ClassifiableIDType;
import de.drv.dsrv.extrastandard.namespace.components.DataType;
import de.drv.dsrv.extrastandard.namespace.components.FlagCodeType;
import de.drv.dsrv.extrastandard.namespace.components.FlagType;
import de.drv.dsrv.extrastandard.namespace.components.ReportType;
import de.drv.dsrv.extrastandard.namespace.components.RequestDetailsType;
import de.drv.dsrv.extrastandard.namespace.components.ResponseDetailsType;
import de.drv.dsrv.extrastandard.namespace.response.TransportBody;
import de.drv.dsrv.extrastandard.namespace.response.TransportHeader;
import de.drv.dsrv.extrastandard.namespace.response.XMLTransport;
import de.extra.client.core.observer.TransportInfoBuilder;
import de.extrastandard.api.observer.ITransportInfo;
import de.extrastandard.api.observer.ITransportObserver;
import de.extrastandard.api.plugin.IOutputPlugin;

/**
 * Setzt die erwarteten Felder für ein FetchResponse
 * 
 * @author Leonid Potap
 * @version $Id$
 */
@Named("dummyDataResponceOutputPlugin")
public class DummyDataResponceOutputPlugin implements IOutputPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(DummyDataResponceOutputPlugin.class);

	@Inject
	@Named("eXTrajaxb2Marshaller")
	private Marshaller marshaller;

	@Inject
	@Named("transportObserver")
	private ITransportObserver transportObserver;

	@Inject
	@Named("transportInfoBuilder")
	private TransportInfoBuilder transportInfoBuilder;

	@Override
	public InputStream outputData(final InputStream request) {
		InputStream responseAsinputStream = null;
		try {
			LOG.info("request={}", request);
			final XMLTransport response = createExtraResponse(request);

			final Writer writer = new StringWriter();
			final StreamResult streamResult = new StreamResult(writer);

			marshaller.marshal(response, streamResult);

			responseAsinputStream = new ByteArrayInputStream(writer.toString().getBytes());
			return responseAsinputStream;
		} catch (final IOException ioException) {
			// Hier kommt eine ExtraTechnischeRuntimeException
			LOG.error("Unerwarteter Fehler: ", ioException);
		}
		return responseAsinputStream;

	}

	private XMLTransport createExtraResponse(final InputStream request) {
		final XMLTransport response = new XMLTransport();
		final TransportHeader transportHeader = new TransportHeader();
		final ResponseDetailsType responseDetailsType = new ResponseDetailsType();
		final ClassifiableIDType idType = new ClassifiableIDType();
		idType.setValue("42");
		responseDetailsType.setResponseID(idType);
		final ReportType reportType = new ReportType();
		final FlagType flagType = new FlagType();
		final FlagCodeType flagCodeType = new FlagCodeType();
		flagCodeType.setValue("C00");
		flagType.setCode(flagCodeType);
		reportType.getFlag().add(flagType);
		responseDetailsType.setReport(reportType);
		transportHeader.setResponseDetails(responseDetailsType);
		final RequestDetailsType requestDetailsType = new RequestDetailsType();
		final ClassifiableIDType requestIdType = new ClassifiableIDType();
		requestIdType.setValue("4242");
		requestDetailsType.setRequestID(requestIdType);
		transportHeader.setRequestDetails(requestDetailsType);
		response.setTransportHeader(transportHeader);

		final TransportBody transportBody = createDummyBodyResponse();
		response.setTransportBody(transportBody);
		final de.drv.dsrv.extrastandard.namespace.request.TransportHeader requestHeader = new de.drv.dsrv.extrastandard.namespace.request.TransportHeader();
		requestHeader.setRequestDetails(requestDetailsType);
		final ITransportInfo transportInfo = transportInfoBuilder.createTransportInfo(requestHeader);
		transportObserver.requestFilled(transportInfo);
		transportObserver.requestForwarded("dummy, keine Weiterleitung", 0);

		return response;
	}

	/**
	 * @return Dummy Body Response
	 */
	private TransportBody createDummyBodyResponse() {
		final TransportBody transportBody = new TransportBody();
		final DataType value = new DataType();
		final String stringValue = "DUMMY Responce";
		final byte[] decodeBase64Value = Base64.encodeBase64(stringValue.getBytes());

		final Base64CharSequenceType base64CharSequenceType = new Base64CharSequenceType();
		base64CharSequenceType.setValue(decodeBase64Value);
		value.setBase64CharSequence(base64CharSequenceType);
		transportBody.setData(value);
		return transportBody;
	}
}