/**
 * HockeyIonSender.java
 * Hockey ACRA-to-HockeyApp crash report sender based on Ion. (https://github.com/koush/ion)
 * <p>
 * To the extent possible under law, Tomasz J Góralczyk has waived all copyright and related
 * or neighboring rights to HockeyIonSender.java.
 * <p>
 * This work is published from: United Kingdom.
 */
package io.github.tjg1.nori.util;


import android.content.Context;

import androidx.annotation.NonNull;

import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.config.ACRAConfiguration;
import org.acra.model.Element;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.acra.sender.ReportSenderFactory;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.github.tjg1.nori.BuildConfig;

import static org.acra.ReportField.ANDROID_VERSION;
import static org.acra.ReportField.APP_VERSION_CODE;
import static org.acra.ReportField.BRAND;
import static org.acra.ReportField.INSTALLATION_ID;
import static org.acra.ReportField.PACKAGE_NAME;
import static org.acra.ReportField.PHONE_MODEL;
import static org.acra.ReportField.STACK_TRACE;
import static org.acra.ReportField.USER_COMMENT;
import static org.acra.ReportField.USER_EMAIL;

;

public class HockeyIonSender implements ReportSender {
    private static final String BASE_URL = "https://rink.hockeyapp.net/api/2/apps/";
    private static final String CRASHES_PATH = "/crashes/";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ",
            Locale.UK);

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData crashReportData)
            throws ReportSenderException {
        try {
            Response<String> response = Ion.with(context)
                    .load("POST", BASE_URL + BuildConfig.HOCKEYAPP_APP_ID + CRASHES_PATH)
                    .setBodyParameter("raw", createCrashLog(crashReportData))
                    .setBodyParameter("userID", crashReportData.getProperty(INSTALLATION_ID))
                    .setBodyParameter("contact", crashReportData.getProperty(USER_EMAIL))
                    .setBodyParameter("description", crashReportData.getProperty(USER_COMMENT))
                    .asString()
                    .withResponse()
                    .get(60, TimeUnit.SECONDS);

            int responseCode = response.getHeaders().code();
            if (responseCode != 201 && responseCode != 202) {
                throw new ReportSenderException("Failed to submit crash data with response code: "
                        + String.valueOf(responseCode));
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new ReportSenderException("Failed to submit crash data.", e);
        }
    }

    /**
     * Convert ACRA {@link CrashReportData} to a String.
     *
     * @param report Crash report.
     * @return Formatted string with crash report data.
     */
    private String createCrashLog(CrashReportData report) {
        StringBuilder log = new StringBuilder();

        log.append("Package: ").append(report.get(PACKAGE_NAME)).append("\n");
        log.append("Version: ").append(report.get(APP_VERSION_CODE)).append("\n");
        log.append("Flavor: ").append(BuildConfig.FLAVOR).append("\n");
        log.append("Android: ").append(report.get(ANDROID_VERSION)).append("\n");
        log.append("Manufacturer: ").append(report.get(BRAND)).append("\n");
        log.append("Model: ").append(report.get(PHONE_MODEL)).append("\n");
        log.append("Date: ").append(DATE_FORMAT.format(GregorianCalendar.getInstance().getTime()))
                .append("\n");
        log.append("\n");
        log.append(report.get(STACK_TRACE));
        log.append("\n");

        // Print one-liners first
        for (Map.Entry<ReportField, Element> reportField : report.entrySet()) {
            String value = reportField.getValue().toString();
            if (!value.endsWith("\n")) {
                log.append(reportField.getKey().name()).append(": ").append(value).append("\n");
            }
        }

        // Then print big items
        for (Map.Entry<ReportField, Element> reportField : report.entrySet()) {
            String value = reportField.getValue().toString();
            if (value.endsWith("\n")) {
                log.append("-------------------------------------------------------------------------------------------\n");
                log.append(reportField.getKey().name()).append(":\n");
                log.append(value);
            }
        }
        return log.toString();
    }

    public static class SenderFactory implements ReportSenderFactory {

        @NonNull
        @Override
        public ReportSender create(@NonNull Context context,
                                   @NonNull ACRAConfiguration acraConfiguration) {
            return new HockeyIonSender();
        }
    }
}
