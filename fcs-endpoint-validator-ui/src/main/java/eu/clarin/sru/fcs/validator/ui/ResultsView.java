package eu.clarin.sru.fcs.validator.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.logging.log4j.core.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;

import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationRequest;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationResponse;
import eu.clarin.sru.fcs.validator.FCSTestResult;
import eu.clarin.sru.fcs.validator.HttpRequestResponseInfo;

public class ResultsView extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(ResultsView.class);

    // DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static final String PATH_PREFIX_RESULTS = "results/";

    private final FCSEndpointValidatorService fcsEndpointValidatorService;
    private final FCSEndpointValidationResult result;

    public Button btnSaveResults = null;

    public ResultsView(FCSEndpointValidationResult result, FCSEndpointValidatorService fcsEndpointValidatorService) {
        this.result = result;
        this.fcsEndpointValidatorService = fcsEndpointValidatorService;

        // --------------------------------------------------------

        addClassName(Gap.XSMALL);
        setWidth("100%");
        setHeight("100%");
        getStyle()
                .set("flex-grow", "1")
                .set("margin-block", "0")
                .set("paddding-block", "0");

        // --------------------------------------------------------

        add(createResultsHeader());
        add(createResultsDetails());

        setupEventHandlers();
    }

    // ----------------------------------------------------------------------
    // event handlers

    protected void setupEventHandlers() {
        if (btnSaveResults != null && fcsEndpointValidatorService != null
                && fcsEndpointValidatorService.canStoreFCSEndpointValidationResults()) {
            btnSaveResults.addClickListener(ce -> {
                final Dialog dlgSaveResults = createSaveResultsAskForUserInfosDialog();
                dlgSaveResults.addOpenedChangeListener(oce -> {
                    if (!oce.getSource().isOpened()) {
                        if (result.isSaved()) {
                            btnSaveResults.setText("Saved!");
                            btnSaveResults.setEnabled(false);
                        } else {
                            btnSaveResults.setEnabled(true);
                        }
                    }
                });
                dlgSaveResults.open();
            });
        }
    }

    // ----------------------------------------------------------------------

    public void showNotificationResultId(final String resultId) {
        Notification notification = new Notification();
        notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        notification.setDuration(15000);

        Button closeButton = new Button(new Icon("lumo", "cross"));
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        closeButton.setAriaLabel("Close");
        closeButton.addClickListener(event -> {
            notification.close();
        });

        Span msg = new Span();
        msg.add("Your endpoint validation result was saved temporarily.");
        msg.add(" You can retrieve it for a limited period of time from ");
        msg.add(new Anchor(PATH_PREFIX_RESULTS + resultId, resultId));
        msg.add(".");

        HorizontalLayout layout = new HorizontalLayout(msg, closeButton);
        notification.add(layout);

        notification.open();
    }

    // ----------------------------------------------------------------------

    protected Component createResultsHeader() {
        HorizontalLayout hlResultsHeader = new HorizontalLayout();
        hlResultsHeader.setSpacing(false);
        hlResultsHeader.setPadding(false);
        hlResultsHeader.addClassName(Gap.XSMALL);
        hlResultsHeader.setWidth("100%");
        hlResultsHeader.setHeight("min-content");
        hlResultsHeader.setJustifyContentMode(JustifyContentMode.CENTER);
        hlResultsHeader.setAlignItems(Alignment.CENTER);

        VerticalLayout vlSummary = new VerticalLayout();
        vlSummary.setPadding(false);
        vlSummary.addClassName(Gap.XSMALL);
        vlSummary.add(createResultsSummary());
        hlResultsHeader.add(vlSummary);

        if (fcsEndpointValidatorService != null && fcsEndpointValidatorService.canStoreFCSEndpointValidationResults()) {
            hlResultsHeader.add(createActionButtons());
        }

        return hlResultsHeader;
    }

    protected Component createActionButtons() {
        VerticalLayout vlButtons = new VerticalLayout();
        vlButtons.addClassName(Gap.XSMALL);
        vlButtons.addClassName(Padding.XSMALL);
        vlButtons.setWidth("min-content");
        vlButtons.setHeight("min-content");
        vlButtons.setJustifyContentMode(JustifyContentMode.START);
        vlButtons.setAlignItems(Alignment.CENTER);

        btnSaveResults = new Button();
        btnSaveResults.setText("Save Results");
        btnSaveResults.setWidth("min-content");
        btnSaveResults.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnSaveResults.setIcon(VaadinIcon.BOOKMARK.create());
        btnSaveResults.setDisableOnClick(true);
        btnSaveResults.addClickShortcut(Key.SAVE);
        vlButtons.add(btnSaveResults);

        if (result.isSaved()) {
            btnSaveResults.setText("Already saved!");
            btnSaveResults.setEnabled(false);
        }

        return vlButtons;
    }

    protected List<Component> createResultsSummary() {
        FCSEndpointValidationRequest request = result.getRequest();
        FCSEndpointValidationResponse response = result.getResponse();

        H2 txtResultsFor = new H2();
        txtResultsFor.add("Result for ");
        txtResultsFor.add(
                new Anchor(request.getBaseURI(), request.getBaseURI(), AnchorTarget.BLANK));
        txtResultsFor.add(" (using test profile ");
        txtResultsFor.add(request.getFCSTestProfile().toDisplayString());
        txtResultsFor.add("):");
        txtResultsFor.getStyle().set("font-size", "var(--lumo-font-size-l)");

        final String summary;
        final Icon summaryIcon;
        if (response.getCountFailure() > 0) {
            summary = (response.getCountFailure() == 1) ? "The endpoint fails to pass in one test."
                    : String.format("The endpoint fails to pass in %d tests.", response.getCountFailure());
            summaryIcon = createIconForTestStatus(FCSTestResult.FCSTestResultStatus.FAILED);
        } else if (response.getCountWarning() > 0) {
            summary = (response.getCountWarning() == 1) ? "The endpoint has minor problems to pass one test."
                    : String.format("The endpoint has minor problems to pass %d tests.", response.getCountWarning());
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

        Div txtTestResultCounts = new Div();
        txtTestResultCounts.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.ROW,
                LumoUtility.Gap.SMALL);

        Span txtSuccess = new Span(String.format("Success: %s", response.getCountSuccess()));
        txtSuccess.getElement().getThemeList().add("badge success");
        txtTestResultCounts.add(txtSuccess);

        Span txtWarnings = new Span(String.format("Warnings: %s", response.getCountWarning()));
        txtWarnings.getElement().getThemeList().add("badge");
        txtTestResultCounts.add(txtWarnings);

        Span txtErrors = new Span(String.format("Errors: %s", response.getCountFailure()));
        txtErrors.getElement().getThemeList().add("badge error");
        txtTestResultCounts.add(txtErrors);

        Span txtSkipped = new Span(String.format("Skipped: %s", response.getCountSkipped()));
        txtSkipped.getElement().getThemeList().add("badge contrast");
        txtTestResultCounts.add(txtSkipped);

        // Arrays.asList
        return List.of(txtResultsFor, txtTestResultSummary, txtTestResultCounts);
    }

    protected List<Component> createResultsDetails() {
        H2 txtResultsDetails = new H2("Results for individual test cases:");
        txtResultsDetails.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin-block-start", "2ex");

        Div accordionResultDetails = new Div();
        // accordionResultDetails.close();

        FCSEndpointValidationRequest request = result.getRequest();
        FCSEndpointValidationResponse response = result.getResponse();
        for (FCSTestResult testResult : response.getResultsList()) {
            accordionResultDetails.add(createSingleResultDetails(request, testResult));
        }

        return List.of(txtResultsDetails, accordionResultDetails);
    }

    protected AccordionPanel createSingleResultDetails(FCSEndpointValidationRequest request, FCSTestResult testResult) {
        VerticalLayout resultDetail = new VerticalLayout();
        resultDetail.setSpacing(false);
        resultDetail.setPadding(false);

        Span expectedResult = new Span();
        Span expectedResultLabel = new Span("Expected result: ");
        expectedResultLabel.addClassName(LumoUtility.FontWeight.BOLD);
        expectedResult.add(expectedResultLabel);
        expectedResult.add(testResult.getExpected());
        resultDetail.add(expectedResult);

        Span actualResult = new Span();
        Span actualResultLabel = new Span("Actual result: ");
        actualResultLabel.addClassName(LumoUtility.FontWeight.BOLD);
        actualResult.add(actualResultLabel);
        String message = testResult.getMessage();
        Span actualResultValue = new Span((message != null) ? message : "Test passed without errors.");
        actualResultValue.getStyle().set("font-style", "italic");
        actualResult.add(actualResultValue);
        resultDetail.add(actualResult);

        // details for HTTP stuff

        if (!testResult.getHttpRequestResponseInfos().isEmpty()) {
            H4 txtHTTPHeader = new H4("HTTP requests and responses:");
            txtHTTPHeader.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.FontSize.SMALL);
            resultDetail.add(txtHTTPHeader);

            for (HttpRequestResponseInfo info : testResult.getHttpRequestResponseInfos()) {
                if (info.getRequest() == null) {
                    continue;
                }

                Button btnViewHttp = new Button("View");
                btnViewHttp.setPrefixComponent(VaadinIcon.FILE_CODE.create());
                btnViewHttp.addThemeVariants(ButtonVariant.LUMO_SMALL);

                Span httpInfo = new Span();
                httpInfo.add(btnViewHttp);
                httpInfo.add(String.format(" [%s]: %s %s",
                        Optional.ofNullable(info.getResponse()).filter(Objects::nonNull)
                                .map(r -> r.getStatusLine().getStatusCode()).orElse(-1),
                        info.getRequest().getRequestLine().getMethod(), info.getRequest().getRequestLine().getUri()));
                resultDetail.add(httpInfo);

                btnViewHttp.addClickListener(event -> showHttpInfoDialog(info));
            }
        }

        if (testResult.getException() != null) {
            H4 txtLogHeader = new H4("Exception:");
            txtLogHeader.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.FontSize.SMALL);
            resultDetail.add(txtLogHeader);

            Throwable t = testResult.getException();
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

        if (!testResult.getLogs().isEmpty()) {
            H4 txtLogHeader = new H4("Debug messages:");
            txtLogHeader.addClassName(LumoUtility.Margin.Top.MEDIUM);
            txtLogHeader.addClassName(LumoUtility.FontSize.SMALL);
            resultDetail.add(txtLogHeader);

            Div resultDetailLogs = new Div();
            resultDetailLogs.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                    LumoUtility.AlignItems.START, LumoUtility.BoxSizing.BORDER, LumoUtility.FontSize.SMALL,
                    LumoUtility.TextColor.TERTIARY);

            for (LogEvent log : testResult.getLogs()) {
                resultDetailLogs.add(new Span(String.format("[%s] %s",
                        dateFmt.format(Instant.ofEpochMilli(log.getTimeMillis()).atZone(ZoneId.systemDefault())),
                        log.getMessage().getFormattedMessage())));
            }

            resultDetail.add(resultDetailLogs);
        }

        AccordionPanel pnlResultDetail = new AccordionPanel();
        // border-bottom: solid 1px var(--lumo-contrast-10pct);
        Span pnlResultDetailSummary = new Span(); // h3 ?

        Icon iconStatus = createIconForTestStatus(testResult.getStatus());
        iconStatus.getStyle().set("vertical-align", "bottom");
        pnlResultDetailSummary.add(iconStatus);
        pnlResultDetailSummary.add(" ");

        Span badgeTestProfile = new Span(request.getFCSTestProfile().toDisplayString());
        badgeTestProfile.getElement().getThemeList().add("badge contrast");
        pnlResultDetailSummary.add(badgeTestProfile);
        pnlResultDetailSummary.add(" ");

        Span badgeCategory = new Span(testResult.getCategory());
        badgeCategory.getElement().getThemeList().add("badge contrast");
        pnlResultDetailSummary.add(badgeCategory);
        pnlResultDetailSummary.add(" ");

        pnlResultDetailSummary.add(testResult.getName());
        pnlResultDetail.setSummary(pnlResultDetailSummary);
        pnlResultDetail.add(resultDetail);

        return pnlResultDetail;
    }

    private Dialog showHttpInfoDialog(HttpRequestResponseInfo info) {
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
        url.add(new Anchor(info.getRequest().getRequestLine().getUri(), info.getRequest().getRequestLine().getUri(),
                AnchorTarget.BLANK));
        layoutViewCodeMeta.add(url);
        if (info.getRequest().getAllHeaders().length > 0) {
            layoutViewCodeMeta
                    .add(new Span(String.format("Headers: %s", Arrays.toString(info.getRequest().getAllHeaders()))));
        }

        H3 txtHeaderResponse = new H3("Response");
        txtHeaderResponse.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.Top.MEDIUM);
        layoutViewCodeMeta.add(txtHeaderResponse);
        if (info.getResponse() != null) {
            layoutViewCodeMeta
                    .add(new Span(String.format("Status: %s", info.getResponse().getStatusLine().toString())));
            layoutViewCodeMeta
                    .add(new Span(String.format("Headers: %s", Arrays.toString(info.getResponse().getAllHeaders()))));
        } else {
            layoutViewCodeMeta.add(new Span("No response could be found!"));
        }
        viewCodeDialog.add(layoutViewCodeMeta);

        if (info.getResponseBytes() != null) {
            AceEditor ace = new AceEditor();
            ace.setTheme(AceTheme.github);
            ace.setMode(AceMode.xml);
            ace.setReadOnly(true);
            ace.setWrap(true);
            ace.setValue(getByteAsString(info.getResponseBytes()));
            ace.addClassName(LumoUtility.Margin.Top.MEDIUM);
            viewCodeDialog.add(ace);
        }

        viewCodeDialog.open();

        return viewCodeDialog;
    }

    protected Dialog createSaveResultsAskForUserInfosDialog() {
        Dialog dlgSaveResultInputs = new Dialog();
        dlgSaveResultInputs.setHeaderTitle("Save FCS Endpoint Validation Result");
        dlgSaveResultInputs.setModal(true);

        Button closeButton = new Button(new Icon("lumo", "cross"), (e) -> dlgSaveResultInputs.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dlgSaveResultInputs.getHeader().add(closeButton);

        // some details

        dlgSaveResultInputs.add(new Span(
                String.format("Validation request from %s.", result.getDatetime().truncatedTo(ChronoUnit.SECONDS))));

        // inputs

        FormLayout flInputs = new FormLayout();
        flInputs.setMaxWidth("30rem");
        flInputs.setResponsiveSteps(new ResponsiveStep("0", 1));

        TextField txtTitle = new TextField("Validation Result Title");
        txtTitle.setWidth("100%");
        txtTitle.focus();
        txtTitle.setClearButtonVisible(true);
        txtTitle.setRequiredIndicatorVisible(true);
        // txtTitle.setPrefixComponent(VaadinIcon.LINK.create());
        txtTitle.setMaxLength(255);
        txtTitle.setPlaceholder("Please enter a short title for your validation result.");
        flInputs.add(txtTitle);

        TextArea txtDescription = new TextArea("Description");
        txtDescription.setClearButtonVisible(true);
        txtDescription.setPlaceholder("Optionally add an longer description for your validation results.");
        flInputs.add(txtDescription);

        dlgSaveResultInputs.add(flInputs);

        // buttons

        Button saveButton = new Button();
        saveButton.setText("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button();
        cancelButton.setText("Cancel");

        dlgSaveResultInputs.getFooter().add(cancelButton);
        dlgSaveResultInputs.getFooter().add(saveButton);

        cancelButton.addClickListener(e -> dlgSaveResultInputs.close());
        // TODO: or create custom event and extract save logic
        saveButton.addClickListener(e -> {
            logger.info("Save results ...");

            if (!txtTitle.getValue().isBlank()) {
                result.setTitle(txtTitle.getValue().trim());
            }
            if (!txtDescription.getValue().isBlank()) {
                result.setDescription(txtDescription.getValue().trim());
            }

            final String resultId = fcsEndpointValidatorService.storeFCSEndpointValidationResult(result);
            if (resultId != null) {
                showNotificationResultId(resultId);
            }
            dlgSaveResultInputs.close();
        });

        return dlgSaveResultInputs;
    }

    // ----------------------------------------------------------------------

    private static String getByteAsString(byte[] bytes) {
        String encoding = Optional.ofNullable(detectEncoding(bytes)).orElse(StandardCharsets.UTF_8.name());
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            return new String(bytes);
        }
    }

    private static String detectEncoding(byte[] bytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        InputStreamReader isr = new InputStreamReader(bais);
        try {
            XMLStreamReader xsr = XMLInputFactory.newInstance().createXMLStreamReader(isr);
            return Optional.ofNullable(xsr.getCharacterEncodingScheme()).orElse(xsr.getEncoding());
        } catch (XMLStreamException | FactoryConfigurationError e) {
            return null;
        }
    }

    // ----------------------------------------------------------------------

    // https://vaadin.com/docs/latest/components/badge
    private static Icon createIcon(VaadinIcon vaadinIcon, String label) {
        Icon icon = vaadinIcon.create();
        // icon.addClassName(Padding.XSMALL);
        icon.getStyle().set("padding", "var(--lumo-space-xs");
        // Accessible label
        icon.getElement().setAttribute("aria-label", label);
        // Tooltip
        icon.getElement().setAttribute("title", label);
        return icon;
    }

    private static Icon createIconForTestStatus(FCSTestResult.FCSTestResultStatus status) {
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
