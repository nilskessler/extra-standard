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
package de.extra.client.plugins.dataplugin.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import de.extra.client.core.model.CompressionPluginBean;
import de.extra.client.core.model.DataSourcePluginBean;
import de.extra.client.core.model.EncryptionPluginBean;
import de.extra.client.core.model.PlugindatenBean;
import de.extra.client.core.model.SenderDataBean;
import de.extra.client.plugins.dataplugin.auftragssatz.AuftragssatzType;
import de.extra.client.plugins.dataplugin.auftragssatz.CompressionInfoType;
import de.extra.client.plugins.dataplugin.auftragssatz.DataSourceInfoType;
import de.extra.client.plugins.dataplugin.auftragssatz.EncryptionInfoType;

@Named("dataPluginHelper")
public class DataPluginHelper {

	@Value("${inputVerzeichnis}")
	private String inputDirectory;

	private final Logger logger = Logger.getLogger(DataPluginHelper.class);

	/**
	 * Laedt die Nutzdatenfiles aus dem Input-Verzeichnis.
	 * 
	 * @return Liste mit den Pfaden der Nutzdateien
	 */
	public List<String> getNutzfiles() {
		List<String> worklist = new ArrayList<String>();
		File[] files = new File(inputDirectory).listFiles();

		// Prüfe auf gueltige Dateien
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {

				// Nur Nutzdaten hinzufügen
				if (!files[i].getName().endsWith(".auf")) {
					if (logger.isDebugEnabled()) {
						logger.debug("Nutzdatensatz gefunden");
					}

					worklist.add(files[i].getAbsolutePath());
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Auftragssatz gefunden");
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Datei " + files[i].getAbsolutePath() + " ("
							+ files[i].length()
							+ "Bytes) zur Arbeitsliste hinzugef�gt.");
				}
			}
		}
		logger.info("Arbeitsliste mit " + worklist.size()
				+ " Elementen angelegt.");
		return worklist;
	}

	/**
	 * Wandelt die Nutzdatei in ein Byte-Array für den Aufbau des Requests um.
	 * 
	 * @param filename
	 *            Name der Datei
	 * @return Byte-Array mit den Nutzdaten
	 */
	public byte[] getNutzdaten(String filename) {
		byte[] nutzdaten = null;

		File nutzdatei = new File(filename);
		FileInputStream fis = null;

		if (nutzdatei.exists() && !nutzdatei.isDirectory()) {
			try {
				logger.debug("Einlesen der Nutzdaten");

				fis = new FileInputStream(nutzdatei);
				nutzdaten = new byte[(int) nutzdatei.length()];
				fis.read(nutzdaten);

				if (logger.isTraceEnabled()) {
					logger.trace("Nutzdaten: " + new String(nutzdaten));
				}
			} catch (FileNotFoundException e) {
				logger.error("Datei konnte nicht gefunden werden", e);
			} catch (IOException e) {
				logger.error("Fehler beim Lesen der Datei", e);
			} finally {
				try {
					fis.close();
				} catch (IOException e) {
					logger.error("Fehler beim Schlie�en des Streams", e);
				}
			}
		} else {
			logger.info("Datei nicht vorhanden oder Verzeichnis");
		}

		return nutzdaten;
	}

	/**
	 * Hilfsklasse zum unmarshalling des Auftragssatzes.
	 * 
	 * @param auftragssatzName
	 * @return JaxB-Element vom Typ AuftragssatzType
	 */
	public AuftragssatzType unmarshalAuftragssatz(String auftragssatzName) {
		JAXBContext jc;
		JAXBElement<?> element = null;
		try {
			// Initialisieren des JaxB-Contextes
			jc = JAXBContext
					.newInstance("de.extra.client.plugins.dataPlugin.auftragssatz");

			// Aufruf des Unmarshallers
			Unmarshaller u = jc.createUnmarshaller();
			File auftragsFile = new File(auftragssatzName);
			element = (JAXBElement<?>) u.unmarshal(auftragsFile);
		} catch (JAXBException e) {
			logger.error("Fehler beim Verarbeiten des XML", e);
		} catch (Exception e) {
			logger.error("Fehler beim Verarbeiten des XML", e);
		}

		return (AuftragssatzType) element.getValue();
	}

	/**
	 * Fuellt die Liste der VersanddatenBean.
	 * 
	 * @param vdb
	 *            VersanddatenBean
	 * @param auftragssatz
	 *            Auftragssatz
	 * @return VersanddatenBean
	 */
	public SenderDataBean fuelleVersandatenBean(SenderDataBean vdb,
			AuftragssatzType auftragssatz) {
		CompressionPluginBean compressionPlugin = new CompressionPluginBean();
		EncryptionPluginBean encryptionPlugin = new EncryptionPluginBean();
		DataSourcePluginBean dataSourcePlugin = new DataSourcePluginBean();

		List<PlugindatenBean> pluginListe = new ArrayList<PlugindatenBean>();
		if (auftragssatz.getCompressionInfo() != null) {

			compressionPlugin = fuelleCompression(auftragssatz
					.getCompressionInfo());
			pluginListe.add(compressionPlugin);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Keine Informationen zur Kompression gefunden");
			}
		}

		if (auftragssatz.getEncryptionInfo() != null) {
			encryptionPlugin = fuelleEncryption(auftragssatz
					.getEncryptionInfo());
			pluginListe.add(encryptionPlugin);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Keine Informationen zur Verschluesselung gefunden");
			}
		}
		if (auftragssatz.getDataSourceInfo() != null) {
			dataSourcePlugin = fuelleDataSource(auftragssatz
					.getDataSourceInfo());
			pluginListe.add(dataSourcePlugin);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Keine Informationen zur DataSource gefunden");
			}
		}

		vdb.setPlugins(pluginListe);
		return vdb;
	}

	/**
	 * Hilfsklasse zum Befüllen der CompressionPluginBean.
	 * 
	 * @param compressionInfo
	 * @return CompressionPluginBean
	 */
	private static CompressionPluginBean fuelleCompression(
			CompressionInfoType compressionInfo) {
		CompressionPluginBean compressionPlugin = new CompressionPluginBean();

		compressionPlugin.setOrder(compressionInfo.getOrder().intValue());
		compressionPlugin.setCompAlgoId(compressionInfo.getAlgoId());
		compressionPlugin.setCompAlgoVers(compressionInfo.getAlgoVersion());
		compressionPlugin.setCompAlgoName(compressionInfo.getAlgoName());
		compressionPlugin.setCompSpecUrl(compressionInfo.getSpecUrl());
		compressionPlugin.setCompSpecName(compressionInfo.getSpecName());
		compressionPlugin.setCompSpecVers(compressionInfo.getSpecVers());
		compressionPlugin.setCompInput(compressionInfo.getInputSize()
				.intValue());
		compressionPlugin.setCompOutput(compressionInfo.getOutputSize()
				.intValue());

		return compressionPlugin;
	}

	/**
	 * Hilfsklasse zum Befüllen der EncryprionPluginBean.
	 * 
	 * @param encryptionInfo
	 * @return EncryptionPluginBean
	 */
	private static EncryptionPluginBean fuelleEncryption(
			EncryptionInfoType encryptionInfo) {
		EncryptionPluginBean encryptionPlugin = new EncryptionPluginBean();

		encryptionPlugin.setOrder(encryptionInfo.getOrder().intValue());
		encryptionPlugin.setEncAlgoId(encryptionInfo.getAlgoId());
		encryptionPlugin.setEncAlgoVers(encryptionInfo.getAlgoVersion());
		encryptionPlugin.setEncAlgoName(encryptionInfo.getAlgoName());
		encryptionPlugin.setEncSpecUrl(encryptionInfo.getSpecUrl());
		encryptionPlugin.setEncSpecName(encryptionInfo.getSpecName());
		encryptionPlugin.setEncSpecVers(encryptionInfo.getSpecVers());
		encryptionPlugin.setEncInput(encryptionInfo.getInputSize().intValue());
		encryptionPlugin
				.setEncOutput(encryptionInfo.getOutputSize().intValue());

		return encryptionPlugin;
	}

	/**
	 * Hilfsklasse zum Befüllen der DataSourcePlugin.
	 * 
	 * @param dataSource
	 * @return
	 */
	private static DataSourcePluginBean fuelleDataSource(
			DataSourceInfoType dataSource) {
		DataSourcePluginBean dataSourcePlugin = new DataSourcePluginBean();

		dataSourcePlugin.setDsType(dataSource.getDsType());
		dataSourcePlugin.setDsName(dataSource.getDsName());
		dataSourcePlugin.setDsCreated(dataSource.getDsCreateDate()
				.toGregorianCalendar().getTime());
		dataSourcePlugin.setDsEncoding(dataSource.getDsEncoding());

		return dataSourcePlugin;
	}
}
