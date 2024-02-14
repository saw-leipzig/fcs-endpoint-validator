package eu.clarin.sru.fcs.tester.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
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
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.Scroller.ScrollDirection;
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

@PageTitle("FCS SRU Endpoint Conformance Tester")
@Route
@Uses(Icon.class)
@JsModule("./prefers-color-scheme.js")
// @JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
public class MainView extends VerticalLayout {

    public VerticalLayout mainContent;
    public TextField txtEndpointURL;
    public TextField txtSearchTerm;
    public Button btnStart;
    public Button btnConfig;

    // ----------------------------------------------------------------------

    public MainView() {

        // main content area (scrollable)
        Scroller mainContentScroller = new Scroller();
        mainContentScroller.setWidth("100%");
        mainContentScroller.setHeight("100%");
        mainContentScroller.getStyle()
                .set("flex-grow", "1")
                .set("padding-block", "0");
        mainContentScroller.setScrollDirection(ScrollDirection.VERTICAL);
        mainContent = new VerticalLayout();
        mainContent.addClassName(Gap.XSMALL);
        mainContent.setWidth("100%");
        mainContent.setHeight("100%");
        mainContent.getStyle().set("flex-grow", "1");
        mainContentScroller.setContent(mainContent);

        // --------------------------------------------------------
        // main content

        mainContent.removeAll();
        mainContent.add(createNoResultsPlaceholder());

        // demo for later http response inspection
        AceEditor ace = new AceEditor();
        ace.setTheme(AceTheme.github);
        ace.setMode(AceMode.xml);
        ace.setReadOnly(true);
        ace.setValue("<?xml version='1.0' encoding='utf-8'?>\n...");
        mainContent.add(ace);

        // --------------------------------------------------------
        // compose all

        setWidth("100%");
        setHeight("100%");
        // setSpacing(false);
        // setPadding(false);
        getStyle().set("flex-grow", "1");
        getStyle().set("padding-bottom", "0");

        add(createHeader());
        add(createUserInputArea());
        add(mainContentScroller);
        add(createFooter());

        btnStart.addClickShortcut(Key.ENTER);
        btnStart.addClickListener(event -> {
            // input field validation should happen automatically

            final UI ui = UI.getCurrent();

            setMainContentNoResults();

            // mainContent = (VerticalLayout) createResultsContent();
            // mainContentScroller.setContent(mainContent);

        });

        btnConfig.addClickListener(event -> {
            setMainContentNoResults();
            btnStart.setEnabled(true);
        });
    }

    // ----------------------------------------------------------------------

    public void setMainContentNoResults() {
        mainContent.removeAll();
        mainContent.add(createNoResultsPlaceholder());
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
        footerRow.addClassName(Gap.XSMALL);
        footerRow.addClassName(Padding.XSMALL);
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

    public Component createResultsContent() {
        VerticalLayout mainContent = new VerticalLayout();
        mainContent.addClassName(Gap.XSMALL);
        mainContent.setWidth("100%");
        mainContent.setHeight("100%");
        mainContent.getStyle()
                .set("flex-grow", "1")
                .set("margin-block", "0")
                .set("paddding-block", "0");

        mainContent.add(createResultsSummary());
        mainContent.add(createResultsDetails());

        return mainContent;
    }

    public List<Component> createResultsSummary() {
        H2 txtResultsFor = new H2();
        txtResultsFor.add("Result for ");
        txtResultsFor.add(new Anchor("https://fcs.data.saw-leipzig.de/dict", "https://fcs.data.saw-leipzig.de/dict"));
        txtResultsFor.add(" (using test profile ");
        txtResultsFor.add("CLARIN FCS 2.0");
        txtResultsFor.add("):");
        txtResultsFor.getStyle().set("font-size", "var(--lumo-font-size-l)");

        Span txtTestResultSummary = new Span();
        Icon icoExlamation = VaadinIcon.EXCLAMATION.create();
        icoExlamation.setColor("var(--lumo-error-color)");
        icoExlamation.setSize("var(--lumo-icon-size-s)");
        txtTestResultSummary.add(icoExlamation);
        txtTestResultSummary.add("The endpoint fails to pass in 2 tests.");
        // txtTestResultSummary.getStyle().set("margin", "0");

        Span txtTestResultCounts = new Span();
        // txtTestResultCounts.getStyle().set("margin", "0");
        txtTestResultCounts.add("Success: ");
        txtTestResultCounts.add(Integer.toString(12));
        txtTestResultCounts.add(", Warnings: ");
        txtTestResultCounts.add(Integer.toString(0));
        txtTestResultCounts.add(", Errors: ");
        txtTestResultCounts.add(Integer.toString(2));
        txtTestResultCounts.add(", Skipped: ");
        txtTestResultCounts.add(Integer.toString(1));

        return List.of(txtResultsFor, txtTestResultSummary, txtTestResultCounts);
    }

    public List<Component> createResultsDetails() {
        H2 txtResultsDetails = new H2("Results for individual test cases:");
        txtResultsDetails.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin-block-start", "1ex");

        Accordion accordionResultDetails = new Accordion();
        accordionResultDetails.close();

        accordionResultDetails.add(createSingleResultDetails());

        return List.of(txtResultsDetails, accordionResultDetails);
    }

    public AccordionPanel createSingleResultDetails() {
        VerticalLayout resultDetail = new VerticalLayout();
        resultDetail.setSpacing(false);
        resultDetail.setPadding(false);

        Span expectedResult = new Span();
        expectedResult.add("Expected result: ");
        expectedResult.add("No errors or diagnostics");
        resultDetail.add(expectedResult);

        Span actualResult = new Span();
        actualResult.add("Actual result: ");
        Span actualResultValue = new Span("The test case was processed successfully");
        actualResultValue.getStyle().set("font-style", "italic");
        actualResult.add(actualResultValue);
        resultDetail.add(actualResult);

        resultDetail.add(new Span("Debug messages:")); // h4

        Div resultDetailLogs = new Div();
        resultDetailLogs.getStyle()
                .setColor("var(--lumo-tertiary-text-color)");
        resultDetailLogs.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.AlignItems.START, LumoUtility.BoxSizing.BORDER, LumoUtility.FontSize.SMALL);

        resultDetailLogs.add(new Span(
                "[2024-02-13T23:51:38] performing SRU 1.2 explain request to endpoint \"https://fcs.data.saw-leipzig.de/dict\""));
        resultDetailLogs.add(new Span("[2024-02-13T23:51:38] performing explain request"));
        resultDetailLogs.add(new Span(
                "[2024-02-13T23:51:38] submitting HTTP request: https://fcs.data.saw-leipzig.de/dict?operation=explain&version=1.2&x-fcs-endpoint-description=true"));
        resultDetailLogs.add(new Span("[2024-02-13T23:51:38] parsing 'explain' response (mode = non-strict)"));

        resultDetail.add(resultDetailLogs);

        AccordionPanel pnlResultDetail1 = new AccordionPanel();
        Span pnlResultDetailSummary = new Span(); // h3 ?
        Icon pnlResultDetailSummaryIcon = createIcon(VaadinIcon.CLOSE_SMALL, "Error");
        pnlResultDetailSummaryIcon.getElement().getThemeList().add("badge error");
        pnlResultDetailSummary.add(pnlResultDetailSummaryIcon);
        pnlResultDetailSummary.add(" [CLARIN FCS 2.0] Explain: Regular explain request using default version");
        pnlResultDetail1.setSummary(pnlResultDetailSummary);
        pnlResultDetail1.add(resultDetail);

        return pnlResultDetail1;
    }

    // ----------------------------------------------------------------------

    // https://vaadin.com/docs/latest/components/badge
    private Icon createIcon(VaadinIcon vaadinIcon, String label) {
        Icon icon = vaadinIcon.create();
        icon.getStyle().set("padding", "var(--lumo-space-xs");
        // Accessible label
        icon.getElement().setAttribute("aria-label", label);
        // Tooltip
        icon.getElement().setAttribute("title", label);
        return icon;
    }

}
