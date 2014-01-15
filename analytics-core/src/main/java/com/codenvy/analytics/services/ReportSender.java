/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.analytics.services;

import com.codenvy.analytics.Configurator;
import com.codenvy.analytics.metrics.Parameters;
import com.codenvy.analytics.services.configuration.ConfigurationManagerException;
import com.codenvy.analytics.services.configuration.XmlConfigurationManager;
import com.codenvy.analytics.services.reports.*;
import com.codenvy.analytics.services.view.CSVReportPersister;
import com.codenvy.analytics.services.view.ViewBuilder;
import com.codenvy.analytics.services.view.ViewData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportSender extends Feature {

    private static final Logger LOG = LoggerFactory.getLogger(ReportSender.class);

    private static final String AVAILABLE     = "reports.available";
    private static final String CONFIGURATION = "reports.configuration";
    private static final String MAIL_TEXT     = "reports.mail.text";
    private static final String MAIL_SUBJECT  = "reports.mail.subject";

    private final RecipientsHolder recipientsHolder;
    private final ViewBuilder      viewBuilder;

    public ReportSender() {
        this.recipientsHolder = new RecipientsHolder();
        this.viewBuilder = new ViewBuilder();
    }

    @Override
    protected void putParametersInContext(Map<String, String> context) {
    }

    @Override
    public boolean isAvailable() {
        return Configurator.getBoolean(AVAILABLE);
    }

    protected void doExecute(Map<String, String> context) throws IOException,
                                                                 ParseException,
                                                                 ClassNotFoundException,
                                                                 NoSuchMethodException,
                                                                 InstantiationException,
                                                                 IllegalAccessException,
                                                                 InvocationTargetException {
        LOG.info("ReportSender is started");
        long start = System.currentTimeMillis();

        try {
            ReportsConfiguration configuration = readConfiguration();

            for (ReportConfiguration reportConfiguration : configuration.getReports()) {
                for (FrequencyConfiguration frequencyConfiguration : reportConfiguration.getFrequencies()) {
                    for (AbstractFrequencyConfiguration frequency : frequencyConfiguration.frequencies()) {

                        if (frequency.isAppropriateDateToSendReport(context)) {
                            context = frequency.initContext(context);
                            RecipientsConfiguration recipients = reportConfiguration.getRecipients();

                            sendReport(context,
                                       frequency,
                                       recipients);
                        }
                    }
                }
            }
        } finally {
            LOG.info("ReportSender is finished in " + (System.currentTimeMillis() - start) / 1000 + " sec.");
        }
    }

    protected void sendReport(Map<String, String> context,
                              AbstractFrequencyConfiguration frequency,
                              RecipientsConfiguration recipients) throws IOException,
                                                                         ParseException,
                                                                         ClassNotFoundException,
                                                                         NoSuchMethodException,
                                                                         InvocationTargetException,
                                                                         InstantiationException,
                                                                         IllegalAccessException {

        String subject = Configurator.getString(MAIL_SUBJECT).replace("[period]", frequency.getTimeUnit().name());

        for (String recipient : recipients.getRecipients()) {
            for (String email : recipientsHolder.getEmails(recipient, context)) {
                MailService.Builder builder = new MailService.Builder();
                builder.setText(Configurator.getString(MAIL_TEXT));
                builder.setSubject(subject);
                builder.setTo(email);

                List<File> reports = getReports(context, email, frequency);
                builder.attach(reports);
                builder.build().send();

                LOG.info("Reports " + reports.toString() + " were send to " + email);
            }
        }
    }

    private List<File> getReports(Map<String, String> context,
                                  String recipient,
                                  AbstractFrequencyConfiguration frequency) throws IOException,
                                                                                   ParseException,
                                                                                   ClassNotFoundException,
                                                                                   NoSuchMethodException,
                                                                                   IllegalAccessException,
                                                                                   InvocationTargetException,
                                                                                   InstantiationException {

        List<File> reports = new ArrayList<>();
        ViewsConfiguration viewsConfiguration = frequency.getViews();

        for (String view : viewsConfiguration.getViews()) {
            Parameters.VIEW.cloneAndPut(context, view);
            Parameters.RECIPIENT.put(context, recipient);
            context = putSpecificParameters(context, frequency);

            ViewData viewData = viewBuilder.getViewData(view, context);
            String viewId = recipient + File.separator + view;

            File report = CSVReportPersister.storeData(viewId, viewData, context);
            reports.add(report);
        }

        return reports;
    }

    private Map<String, String> putSpecificParameters(Map<String, String> context,
                                                      AbstractFrequencyConfiguration frequency)
            throws ClassNotFoundException,
                   InstantiationException,
                   IllegalAccessException,
                   InvocationTargetException,
                   NoSuchMethodException {

        String clazzName = frequency.getContextModifier().getClazz();
        Class<?> clazz = Class.forName(clazzName);
        ContextModifier contextModifier = (ContextModifier)clazz.getConstructor().newInstance();

        return contextModifier.update(context);
    }

    private ReportsConfiguration readConfiguration() throws ConfigurationManagerException {
        XmlConfigurationManager<ReportsConfiguration> configurationManager =
                new XmlConfigurationManager<>(ReportsConfiguration.class, Configurator.getString(CONFIGURATION));

        return configurationManager.loadConfiguration();
    }
}
