package eu.clarin.sru.fcs.tester.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.core.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;

import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import eu.clarin.sru.fcs.tester.FCSEndpointValidationRequest;
import eu.clarin.sru.fcs.tester.FCSEndpointValidationResponse;
import eu.clarin.sru.fcs.tester.FCSTestResult;

@PageTitle("FCS SRU Endpoint Conformance Tester")
@Route
@Uses(Icon.class)
@JsModule("./prefers-color-scheme.js")
// @JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
public class MainView extends VerticalLayout {

    protected static final Logger logger = LoggerFactory.getLogger(MainView.class);

    public VerticalLayout mainContent;
    public TextField txtEndpointURL;
    public TextField txtSearchTerm;
    public Button btnStart;
    public Button btnConfig;

    // ----------------------------------------------------------------------

    public MainView() {
        mainContent = new VerticalLayout();
        mainContent.addClassName(Gap.XSMALL);
        mainContent.setWidth("100%");
        mainContent.setHeight("100%");
        mainContent.getStyle()
                .set("flex-grow", "1")
                .set("margin-block", "0")
                .set("paddding-block", "0");

        // --------------------------------------------------------
        // main content

        setMainContentNoResults();

        // // demo for later http response inspection
        // AceEditor ace = new AceEditor();
        // ace.setTheme(AceTheme.github);
        // ace.setMode(AceMode.xml);
        // ace.setReadOnly(true);
        // ace.setValue("<?xml version='1.0' encoding='utf-8'?>\n...");
        // mainContent.add(ace);

        // --------------------------------------------------------
        // compose all

        setWidth("100%");
        // setSpacing(false);
        // setPadding(false);
        getStyle().set("flex-grow", "1");
        addClassNames(LumoUtility.MinHeight.SCREEN, Padding.Bottom.NONE);

        add(createHeader());
        add(createUserInputArea());
        add(mainContent);
        add(createFooter());

        // --------------------------------------------------------
        // event handlers

        btnStart.addClickShortcut(Key.ENTER);
        btnStart.addClickListener(event -> {
            // input field validation should happen automatically

            // build FCS Validation Request
            FCSEndpointValidationRequest request = new FCSEndpointValidationRequest();
            request.setBaseURI(txtEndpointURL.getValue().strip());
            request.setUserSearchTerm(txtSearchTerm.getValue());

            final UI ui = UI.getCurrent();

            FCSEndpointTesterService.getInstance().evalute(request).thenAccept((response) -> {
                ui.access(() -> {
                    // re-enable input for user
                    setInputEnabled(true);
                    // render results
                    setMainContentResults(response);
                });
            });

            setMainContentNoResults();
        });

        btnConfig.addClickListener(event -> {
            btnStart.setEnabled(true);
            setMainContentNoResults();
        });
    }

    // ----------------------------------------------------------------------

    public void setInputEnabled(boolean enabled) {
        btnStart.setEnabled(enabled);
    }

    public void setMainContentNoResults() {
        mainContent.removeAll();
        mainContent.add(createNoResultsPlaceholder());
    }

    public void setMainContentResults(FCSEndpointValidationResponse result) {
        mainContent.removeAll();
        mainContent.add(createResultsContent(result));
    }

    // ----------------------------------------------------------------------

    public Component createHeader() {
        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setSpacing(false);
        titleRow.addClassName(Gap.LARGE);
        titleRow.setWidth("100%");
        titleRow.setHeight("min-content");
        titleRow.setJustifyContentMode(JustifyContentMode.START);
        titleRow.setAlignItems(Alignment.CENTER);

        Icon iconTitle = new Icon();
        titleRow.add(iconTitle);

        H1 lblTitle = new H1();
        lblTitle.setText("FCS SRU Endpoint Conformance Tester");
        lblTitle.setWidth("max-content");
        lblTitle.getStyle().set("font-size", "var(--lumo-font-size-xxl)");
        titleRow.add(lblTitle);

        return titleRow;
    }

    public Component createFooter() {
        HorizontalLayout footerRow = new HorizontalLayout();
        footerRow.addClassNames(Gap.XSMALL, Padding.XSMALL, LumoUtility.Margin.Top.AUTO);
        footerRow.setWidth("100%");
        footerRow.setHeight("min-content");
        footerRow.setJustifyContentMode(JustifyContentMode.CENTER);
        footerRow.setAlignItems(Alignment.CENTER);

        Paragraph txtFooter = new Paragraph();
        txtFooter.setText("For questions or bug reports please contact ");
        txtFooter.setWidth("100%");
        txtFooter.setWidth("max-content");
        txtFooter.getStyle().set("font-size", "var(--lumo-font-size-s)");

        Anchor linkEmail = new Anchor();
        linkEmail.setText("fcs@clarin.eu");
        linkEmail.setHref("mailto:fcs@clarin.eu");
        txtFooter.setWidth("max-content");
        txtFooter.add(linkEmail);

        txtFooter.add(".");

        footerRow.add(txtFooter);

        return footerRow;
    }

    public Component createInputFields() {
        FormLayout flInputs = new FormLayout();
        flInputs.addClassName(Gap.XSMALL);
        flInputs.addClassName(Padding.XSMALL);
        flInputs.setWidth("100%");
        flInputs.setResponsiveSteps(new ResponsiveStep("0", 1));
        flInputs.getStyle().set("--vaadin-form-item-label-width", "10em");

        txtEndpointURL = new TextField();
        txtEndpointURL.setWidth("100%");
        txtEndpointURL.addThemeName("label-left");
        txtEndpointURL.setClearButtonVisible(true);
        txtEndpointURL.setRequiredIndicatorVisible(true);
        txtEndpointURL.setPrefixComponent(VaadinIcon.LINK.create());
        txtEndpointURL.setMaxLength(255);
        txtEndpointURL.setPlaceholder("Please enter BaseURI of endpoint.");
        txtEndpointURL.setErrorMessage("An endpoint BaseURI is required!");
        txtEndpointURL.setManualValidation(true);
        txtEndpointURL.addValueChangeListener(event -> {
            final String value = event.getValue();
            if (Objects.equals(value, "") || value.isBlank()) {
                txtEndpointURL.setInvalid(true);
                txtEndpointURL.setErrorMessage("An endpoint BaseURI is required!");
            } else {
                try {
                    new URL(value);
                    // if ok, then signal
                    txtEndpointURL.setInvalid(false);
                } catch (MalformedURLException e) {
                    // on error, show hint
                    txtEndpointURL.setInvalid(true);
                    txtEndpointURL.setErrorMessage("Invalid URI syntax!");
                }
            }
        });
        flInputs.addFormItem(txtEndpointURL, "Endpoint BaseURL");

        txtSearchTerm = new TextField();
        txtSearchTerm.setWidth("100%");
        txtSearchTerm.addThemeName("label-left");
        txtSearchTerm.setClearButtonVisible(true);
        txtSearchTerm.setRequiredIndicatorVisible(true);
        txtSearchTerm.setPrefixComponent(VaadinIcon.SEARCH.create());
        txtSearchTerm.setMaxLength(64);
        txtSearchTerm.setPlaceholder("Please enter a word that occurs at least once in you data.");
        txtSearchTerm.setErrorMessage("A search term is required!");
        flInputs.addFormItem(txtSearchTerm, "Search Term");

        return flInputs;
    }

    public Component createActionButtons() {
        VerticalLayout vlButtons = new VerticalLayout();
        vlButtons.addClassName(Gap.XSMALL);
        vlButtons.addClassName(Padding.SMALL);
        vlButtons.setWidth("min-content");
        vlButtons.setHeight("min-content");
        vlButtons.setJustifyContentMode(JustifyContentMode.START);
        vlButtons.setAlignItems(Alignment.CENTER);

        btnStart = new Button();
        btnStart.setText("Evaluate");
        btnStart.setWidth("min-content");
        btnStart.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnStart.setIcon(VaadinIcon.DOCTOR.create());
        btnStart.setDisableOnClick(true);
        vlButtons.add(btnStart);

        btnConfig = new Button();
        btnConfig.setText("Configure");
        btnConfig.setWidth("min-content");
        btnConfig.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        btnConfig.setIcon(VaadinIcon.TOOLS.create());
        vlButtons.add(btnConfig);

        return vlButtons;
    }

    public Component createUserInputArea() {
        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setSpacing(false);
        headerRow.addClassName(Gap.XSMALL);
        headerRow.setWidth("100%");
        headerRow.setHeight("min-content");
        headerRow.setJustifyContentMode(JustifyContentMode.CENTER);
        headerRow.setAlignItems(Alignment.CENTER);

        headerRow.add(createInputFields());
        headerRow.add(createActionButtons());

        return headerRow;
    }

    public Component createNoResultsPlaceholder() {
        VerticalLayout lytNoResults = new VerticalLayout();
        lytNoResults.setWidth("100%");
        lytNoResults.setHeight("100%");
        lytNoResults.getStyle().set("flex-grow", "1");
        lytNoResults.setJustifyContentMode(JustifyContentMode.CENTER);
        lytNoResults.setAlignItems(Alignment.CENTER);

        Paragraph txtNoResults = new Paragraph();
        txtNoResults.setText("No results available.");
        txtNoResults.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-secondary-text-color)");

        lytNoResults.add(txtNoResults);

        return lytNoResults;
    }

    public List<Component> createResultsContent(FCSEndpointValidationResponse result) {
        List<Component> components = new ArrayList<>();
        components.addAll(createResultsSummary(result));
        components.addAll(createResultsDetails(result));
        return components;
    }

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
                .set("margin-block-start", "1ex");

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
        actualResult.addClassName(LumoUtility.Margin.Bottom.MEDIUM);
        Span actualResultLabel = new Span("Actual result: ");
        actualResultLabel.addClassName(LumoUtility.FontWeight.BOLD);
        actualResult.add(actualResultLabel);
        String message = result.getMessage();
        Span actualResultValue = new Span((message != null) ? message : "Test passed without errors.");
        actualResultValue.getStyle().set("font-style", "italic");
        actualResult.add(actualResultValue);
        resultDetail.add(actualResult);

        // details for HTTP stuff

        if (!result.getLogs().isEmpty()) {
            H4 txtLogHeader = new H4("Debug messages:");
            txtLogHeader.addClassName(LumoUtility.FontSize.SMALL);
            resultDetail.add(txtLogHeader);

            Div resultDetailLogs = new Div();
            resultDetailLogs.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                    LumoUtility.AlignItems.START, LumoUtility.BoxSizing.BORDER, LumoUtility.FontSize.SMALL,
                    LumoUtility.TextColor.TERTIARY);

            for (LogEvent log : result.getLogs()) {
                resultDetailLogs.add(new Span(
                        String.format("[%s] %s", log.getTimeMillis(), log.getMessage().getFormattedMessage())));
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
