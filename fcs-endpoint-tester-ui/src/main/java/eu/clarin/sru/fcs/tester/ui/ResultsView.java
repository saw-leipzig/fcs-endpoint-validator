package eu.clarin.sru.fcs.tester.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.logging.log4j.core.LogEvent;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;

import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import eu.clarin.sru.fcs.tester.FCSEndpointValidationRequest;
import eu.clarin.sru.fcs.tester.FCSEndpointValidationResponse;
import eu.clarin.sru.fcs.tester.FCSTestResult;
import eu.clarin.sru.fcs.tester.HttpRequestResponseInfo;

public class ResultsView extends VerticalLayout {

    // DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public ResultsView(FCSEndpointValidationResponse result) {

        addClassName(Gap.XSMALL);
        setWidth("100%");
        setHeight("100%");
        getStyle()
                .set("flex-grow", "1")
                .set("margin-block", "0")
                .set("paddding-block", "0");

        // --------------------------------------------------------

        add(createResultsSummary(result));
        add(createResultsDetails(result));

    }

    // ----------------------------------------------------------------------

    public List<Component> createResultsSummary(FCSEndpointValidationResponse result) {
        H2 txtResultsFor = new H2();
        txtResultsFor.add("Result for ");
        txtResultsFor.add(new Anchor(result.getRequest().getBaseURI(), result.getRequest().getBaseURI()));
        txtResultsFor.add(" (using test profile ");
        txtResultsFor.add(result.getRequest().getFCSTestProfile().toDisplayString());
        txtResultsFor.add("):");
        txtResultsFor.getStyle().set("font-size", "var(--lumo-font-size-l)");

        final String summary;
        final Icon summaryIcon;
        if (result.getCountFailure() > 0) {
            summary = (result.getCountFailure() == 1) ? "The endpoint fails to pass in one test."
                    : String.format("The endpoint fails to pass in %d tests.", result.getCountFailure());
            summaryIcon = createIconForTestStatus(FCSTestResult.FCSTestResultStatus.FAILED);
        } else if (result.getCountWarning() > 0) {
            summary = (result.getCountWarning() == 1) ? "The endpoint has minor problems to pass one test."
                    : String.format("The endpoint has minor problems to pass %d tests.", result.getCountWarning());
            summaryIcon = createIconForTestStatus(FCSTestResult.FCSTestResultStatus.WARNING);
        } else {
            summary = "The endpoint passed all tests successfully.";
            summaryIcon = createIconForTestStatus(FCSTestResult.FCSTestResultStatus.SUCCESSFUL);
        }
        Span txtTestResultSummary = new Span();
        txtTestResultSummary.add(summaryIcon);
        txtTestResultSummary.add(" ");
        txtTestResultSummary.add(summary);
        // icoExlamation.setColor("var(--lumo-error-color)");
        // icoExlamation.setSize("var(--lumo-icon-size-s)");
        // txtTestResultSummary.getStyle().set("margin", "0");

        Span txtTestResultCounts = new Span();
        // txtTestResultCounts.getStyle().set("margin", "0");
        txtTestResultCounts.add("Success: ");
        txtTestResultCounts.add(Long.toString(result.getCountSuccess()));
        txtTestResultCounts.add(", Warnings: ");
        txtTestResultCounts.add(Long.toString(result.getCountWarning()));
        txtTestResultCounts.add(", Errors: ");
        txtTestResultCounts.add(Long.toString(result.getCountFailure()));
        txtTestResultCounts.add(", Skipped: ");
        txtTestResultCounts.add(Long.toString(result.getCountSkipped()));

        // Arrays.asList
        return List.of(txtResultsFor, txtTestResultSummary, txtTestResultCounts);
    }

    public List<Component> createResultsDetails(FCSEndpointValidationResponse response) {
        H2 txtResultsDetails = new H2("Results for individual test cases:");
        txtResultsDetails.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin-block-start", "2ex");

        Div accordionResultDetails = new Div();
        // accordionResultDetails.close();

        for (FCSTestResult result : response.getResultsList()) {
            accordionResultDetails.add(createSingleResultDetails(response.getRequest(), result));
        }

        return List.of(txtResultsDetails, accordionResultDetails);
    }

    public AccordionPanel createSingleResultDetails(FCSEndpointValidationRequest request, FCSTestResult result) {
        VerticalLayout resultDetail = new VerticalLayout();
        resultDetail.setSpacing(false);
        resultDetail.setPadding(false);

        Span expectedResult = new Span();
        Span expectedResultLabel = new Span("Expected result: ");
        expectedResultLabel.addClassName(LumoUtility.FontWeight.BOLD);
        expectedResult.add(expectedResultLabel);
        expectedResult.add(result.getExpected());
        resultDetail.add(expectedResult);

        Span actualResult = new Span();
        Span actualResultLabel = new Span("Actual result: ");
        actualResultLabel.addClassName(LumoUtility.FontWeight.BOLD);
        actualResult.add(actualResultLabel);
        String message = result.getMessage();
        Span actualResultValue = new Span((message != null) ? message : "Test passed without errors.");
        actualResultValue.getStyle().set("font-style", "italic");
        actualResult.add(actualResultValue);
        resultDetail.add(actualResult);

        // details for HTTP stuff

        if (!result.getHttpRequestResponseInfos().isEmpty()) {
            H4 txtHTTPHeader = new H4("HTTP requests and responses:");
            txtHTTPHeader.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.FontSize.SMALL);
            resultDetail.add(txtHTTPHeader);

            for (HttpRequestResponseInfo info : result.getHttpRequestResponseInfos()) {
                HttpRequest temp = info.getRequest();
                while (temp instanceof HttpRequestWrapper) {
                    temp = ((HttpRequestWrapper) temp).getOriginal();
                }
                final HttpRequest original = temp;

                Button btnViewHttp = new Button("View");
                btnViewHttp.setPrefixComponent(VaadinIcon.FILE_CODE.create());
                btnViewHttp.addThemeVariants(ButtonVariant.LUMO_SMALL);

                Span httpInfo = new Span();
                httpInfo.add(btnViewHttp);
                httpInfo.add(String.format(" [%s]: %s %s", info.getResponse().getStatusLine().getStatusCode(),
                        original.getRequestLine().getMethod(), original.getRequestLine().getUri()));
                resultDetail.add(httpInfo);

                btnViewHttp.addClickListener(event -> showHttpInfoDialog(info));
            }
        }

        if (result.getException() != null) {
            H4 txtLogHeader = new H4("Exception:");
            txtLogHeader.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.FontSize.SMALL);
            resultDetail.add(txtLogHeader);

            Throwable t = result.getException();
            Pre resultDetailException = new Pre();
            resultDetailException.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                    LumoUtility.AlignItems.START, LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY,
                    LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Padding.Vertical.XSMALL,
                    LumoUtility.Margin.Top.XSMALL);

            Span exceptionTitle = new Span(t.toString());
            exceptionTitle.addClassName(LumoUtility.TextColor.PRIMARY);
            resultDetailException.add(exceptionTitle);
            for (StackTraceElement ste : t.getStackTrace()) {
                if (ste.toString().startsWith("java.base/")) {
                    // stop if we reach internals? (probably)
                    break;
                }
                resultDetailException.add(String.format("\tat %s\n", ste));
            }

            resultDetail.add(resultDetailException);
        }

        if (!result.getLogs().isEmpty()) {
            H4 txtLogHeader = new H4("Debug messages:");
            txtLogHeader.addClassName(LumoUtility.Margin.Top.MEDIUM);
            txtLogHeader.addClassName(LumoUtility.FontSize.SMALL);
            resultDetail.add(txtLogHeader);

            Div resultDetailLogs = new Div();
            resultDetailLogs.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                    LumoUtility.AlignItems.START, LumoUtility.BoxSizing.BORDER, LumoUtility.FontSize.SMALL,
                    LumoUtility.TextColor.TERTIARY);

            for (LogEvent log : result.getLogs()) {
                resultDetailLogs.add(new Span(String.format("[%s] %s",
                        dateFmt.format(Instant.ofEpochMilli(log.getTimeMillis()).atZone(ZoneId.systemDefault())),
                        log.getMessage().getFormattedMessage())));
            }

            resultDetail.add(resultDetailLogs);
        }

        AccordionPanel pnlResultDetail = new AccordionPanel();
        // border-bottom: solid 1px var(--lumo-contrast-10pct);
        Span pnlResultDetailSummary = new Span(); // h3 ?
        pnlResultDetailSummary.add(createIconForTestStatus(result.getStatus()));
        pnlResultDetailSummary.add(" [");
        pnlResultDetailSummary.add(request.getFCSTestProfile().toDisplayString());
        pnlResultDetailSummary.add("] ");
        pnlResultDetailSummary.add(result.getCategory());
        pnlResultDetailSummary.add(": ");
        pnlResultDetailSummary.add(result.getName());
        pnlResultDetail.setSummary(pnlResultDetailSummary);
        pnlResultDetail.add(resultDetail);

        return pnlResultDetail;
    }

    private Dialog showHttpInfoDialog(HttpRequestResponseInfo info) {
        HttpRequest original = info.getRequest();
        while (original instanceof HttpRequestWrapper) {
            original = ((HttpRequestWrapper) original).getOriginal();
        }

        Dialog viewCodeDialog = new Dialog();
        viewCodeDialog.setHeaderTitle("HTTP Request/Response Details");
        viewCodeDialog.setMinWidth(80, Unit.VW);

        Button closeButton = new Button(new Icon("lumo", "cross"), (e) -> viewCodeDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        viewCodeDialog.getHeader().add(closeButton);

        VerticalLayout layoutViewCodeMeta = new VerticalLayout();
        layoutViewCodeMeta.getThemeList().clear();

        H3 txtHeaderRequest = new H3("Request");
        txtHeaderRequest.addClassName(LumoUtility.FontSize.LARGE);
        layoutViewCodeMeta.add(txtHeaderRequest);
        Span url = new Span("URL: ");
        url.add(new Anchor(original.getRequestLine().getUri(), original.getRequestLine().getUri()));
        layoutViewCodeMeta.add(url);
        if (original.getAllHeaders().length > 0) {
            layoutViewCodeMeta.add(new Span(String.format("Headers: %s", Arrays.toString(original.getAllHeaders()))));
        }

        H3 txtHeaderResponse = new H3("Response");
        txtHeaderResponse.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.Top.MEDIUM);
        layoutViewCodeMeta.add(txtHeaderResponse);
        layoutViewCodeMeta.add(new Span(String.format("Status: %s", info.getResponse().getStatusLine().toString())));
        layoutViewCodeMeta
                .add(new Span(String.format("Headers: %s", Arrays.toString(info.getResponse().getAllHeaders()))));

        viewCodeDialog.add(layoutViewCodeMeta);

        AceEditor ace = new AceEditor();
        ace.setTheme(AceTheme.github);
        ace.setMode(AceMode.xml);
        ace.setReadOnly(true);
        ace.setWrap(true);
        ace.setValue(new String(info.getResponseBytes()));
        ace.addClassName(LumoUtility.Margin.Top.MEDIUM);
        viewCodeDialog.add(ace);

        viewCodeDialog.open();

        return viewCodeDialog;
    }

    // ----------------------------------------------------------------------

    // https://vaadin.com/docs/latest/components/badge
    private Icon createIcon(VaadinIcon vaadinIcon, String label) {
        Icon icon = vaadinIcon.create();
        // icon.addClassName(Padding.XSMALL);
        icon.getStyle().set("padding", "var(--lumo-space-xs");
        // Accessible label
        icon.getElement().setAttribute("aria-label", label);
        // Tooltip
        icon.getElement().setAttribute("title", label);
        return icon;
    }

    private Icon createIconForTestStatus(FCSTestResult.FCSTestResultStatus status) {
        if (status == null) {
            Icon icon = createIcon(VaadinIcon.EYE, "Success");
            icon.getElement().getThemeList().add("badge contrast ");
            return icon;
        }

        Icon icon;
        switch (status) {
            case SUCCESSFUL:
                icon = createIcon(VaadinIcon.CHECK, "Success");
                icon.getElement().getThemeList().add("badge success");
                break;
            case FAILED:
                icon = createIcon(VaadinIcon.CLOSE, "Error");
                icon.getElement().getThemeList().add("badge error");
                break;
            case WARNING:
                icon = createIcon(VaadinIcon.EXCLAMATION, "Warning");
                icon.getElement().getThemeList().add("badge");
                break;
            case SKIPPED:
                icon = createIcon(VaadinIcon.CLOUD, "Skipped");
                icon.getElement().getThemeList().add("badge contrast ");
                break;

            default:
                icon = createIcon(VaadinIcon.EYE, "Success");
                icon.getElement().getThemeList().add("badge contrast ");
                break;
        }
        return icon;
    }

}
