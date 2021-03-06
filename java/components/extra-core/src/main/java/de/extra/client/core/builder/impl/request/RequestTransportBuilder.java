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
package de.extra.client.core.builder.impl.request;

import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.drv.dsrv.extrastandard.namespace.request.RequestTransport;
import de.extra.client.core.builder.IRequestTransportBuilder;
import de.extrastandard.api.model.content.IExtraProfileConfiguration;
import de.extrastandard.api.model.content.IInputDataContainer;

/**
 * @author Leonid Potap
 * 
 */
@Named("requestTransportBuilder")
public class RequestTransportBuilder implements IRequestTransportBuilder {

	private static final Logger LOG = LoggerFactory
			.getLogger(RequestTransportBuilder.class);

	private static final String BUILDER_XML_MESSAGE_TYPE = "xcpt:Transport";

	/* (non-Javadoc)
	 * @see de.extra.client.core.builder.impl.request.IRequestTransportBuilder#buildRequestTransport(de.extrastandard.api.model.content.IInputDataContainer, de.extrastandard.api.model.content.IExtraProfileConfiguration)
	 */
	@Override
	public RequestTransport buildRequestTransport(
			final IInputDataContainer senderData,
			final IExtraProfileConfiguration config) {
		return buildRequestTransport(config);
	}

	/* (non-Javadoc)
	 * @see de.extra.client.core.builder.impl.request.IRequestTransportBuilder#buildRequestTransport(de.extrastandard.api.model.content.IExtraProfileConfiguration)
	 */
	@Override
	public RequestTransport buildRequestTransport(
			final IExtraProfileConfiguration config) {
		final RequestTransport requestTransport = new RequestTransport();

		// (11.12.12) Pflichtattribute setzen! (siehe eXTra-Standard)
		requestTransport.setVersion("1.3");
		requestTransport
				.setProfile("http://code.google.com/p/extra-standard/profile/1");

		LOG.debug("Create XML RequestTransport");
		return requestTransport;
	}

	/* (non-Javadoc)
	 * @see de.extra.client.core.builder.impl.request.IRequestTransportBuilder#getXmlType()
	 */
	@Override
	public String getXmlType() {
		return BUILDER_XML_MESSAGE_TYPE;
	}

}
