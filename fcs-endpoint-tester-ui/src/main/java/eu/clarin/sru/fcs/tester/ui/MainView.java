package eu.clarin.sru.fcs.tester.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.Scroller.ScrollDirection;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;

@PageTitle("FCS SRU Endpoint Conformance Tester")
@Route
@Uses(Icon.class)
public class MainView extends Composite<VerticalLayout> {

    public VerticalLayout mainContent;

    public MainView() {
        // header row (input fields)
        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setSpacing(false);
        headerRow.addClassName(Gap.XSMALL);
        headerRow.setWidth("100%");
        headerRow.setHeight("min-content");
        headerRow.setJustifyContentMode(JustifyContentMode.CENTER);
        headerRow.setAlignItems(Alignment.CENTER);

        // main content area (scrollable)
        Scroller mainContentScroller = new Scroller();
        mainContentScroller.setWidth("100%");
        mainContentScroller.setHeight("100%");
        mainContentScroller.getStyle().set("flex-grow", "1");
        mainContentScroller.setScrollDirection(ScrollDirection.VERTICAL);
        mainContent = new VerticalLayout();
        mainContent.addClassName(Gap.XSMALL);
        mainContent.setWidth("100%");
        mainContent.setHeight("100%");
        mainContent.getStyle().set("flex-grow", "1");
        mainContentScroller.setContent(mainContent);

        // --------------------------------------------------------
        // input text fiels

        FormLayout flInputs = new FormLayout();
        flInputs.addClassName(Gap.XSMALL);
        flInputs.addClassName(Padding.XSMALL);
        flInputs.setWidth("100%");
        flInputs.setResponsiveSteps(new ResponsiveStep("0", 1));
        flInputs.getStyle().set("--vaadin-form-item-label-width", "10em");

        TextField txtEndpointURL = new TextField();
        txtEndpointURL.setWidth("100%");
        txtEndpointURL.addThemeName("label-left");
        txtEndpointURL.setClearButtonVisible(true);
        txtEndpointURL.setRequiredIndicatorVisible(true);
        txtEndpointURL.setPrefixComponent(VaadinIcon.LINK.create());
        flInputs.addFormItem(txtEndpointURL, "Endpoint BaseURL");

        TextField txtSearchTerm = new TextField();
        txtSearchTerm.setWidth("100%");
        txtSearchTerm.addThemeName("label-left");
        txtSearchTerm.setClearButtonVisible(true);
        txtSearchTerm.setRequiredIndicatorVisible(true);
        txtSearchTerm.setPrefixComponent(VaadinIcon.SEARCH.create());
        flInputs.addFormItem(txtSearchTerm, "Search Term");

        headerRow.add(flInputs);

        // --------------------------------------------------------
        // input buttons

        VerticalLayout vlButtons = new VerticalLayout();
        vlButtons.addClassName(Gap.XSMALL);
        vlButtons.addClassName(Padding.SMALL);
        vlButtons.setWidth("min-content");
        vlButtons.setHeight("min-content");
        vlButtons.setJustifyContentMode(JustifyContentMode.START);
        vlButtons.setAlignItems(Alignment.CENTER);

        Button btnStart = new Button();
        btnStart.setText("Evaluate");
        btnStart.setWidth("min-content");
        btnStart.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnStart.setIcon(VaadinIcon.DOCTOR.create());
        btnStart.setDisableOnClick(true);
        vlButtons.add(btnStart);

        Button btnConfig = new Button();
        btnConfig.setText("Configure");
        btnConfig.setWidth("min-content");
        btnConfig.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        btnConfig.setIcon(VaadinIcon.TOOLS.create());
        vlButtons.add(btnConfig);

        headerRow.add(vlButtons);

        // --------------------------------------------------------
        // main content

        // mainContent.add(createNoResultsPlaceholder());

        H2 txtResultsFor = new H2();
        txtResultsFor.add("Result for ");
        txtResultsFor.add("https://fcs.data.saw-leipzig.de/dict");
        txtResultsFor.add(" (using test profile ");
        txtResultsFor.add("CLARIN FCS 2.0");
        txtResultsFor.add("):");
        txtResultsFor.getStyle().set("font-size", "var(--lumo-font-size-l)");
        mainContent.add(txtResultsFor);

        Span txtTestResultSummary = new Span();
        Icon icoExlamation = VaadinIcon.EXCLAMATION.create();
        icoExlamation.setColor("var(--lumo-error-color)");
        icoExlamation.setSize("var(--lumo-icon-size-s)");
        txtTestResultSummary.add(icoExlamation);
        txtTestResultSummary.add("The endpoint fails to pass in 2 tests.");
        // txtTestResultSummary.getStyle().set("margin", "0");
        mainContent.add(txtTestResultSummary);

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
        mainContent.add(txtTestResultCounts);

        H2 txtResultsDetails = new H2("Results for individual test cases:");
        txtResultsDetails.getStyle().set("font-size", "var(--lumo-font-size-l)");
        mainContent.add(txtResultsDetails);

        // --------------------------------------------------------
        // compose all

        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");
        getContent().getStyle().set("padding-bottom", "0");
        getContent().setHeight("100%");
        // getContent().setSpacing(false);
        // getContent().setPadding(false);

        getContent().add(createHeader());
        getContent().add(headerRow);
        getContent().add(mainContentScroller);
        getContent().add(createFooter());

        // Button button = new Button("Click me",
        // event -> add(new Paragraph("Clicked!")));

        // add(button);
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
        txtFooter.setText("For questions of bug reports please contact ");
        txtFooter.setWidth("100%");
        txtFooter.setWidth("max-content");
        txtFooter.getStyle().set("font-size", "var(--lumo-font-size-s)");

        Anchor linkEmail = new Anchor();
        linkEmail.setText("fcs@clarin.eu");
        linkEmail.setHref("mailto:fcs@clarin.eu");
        txtFooter.setWidth("max-content");
        txtFooter.add(linkEmail);

        footerRow.add(txtFooter);

        return footerRow;
    }

}
