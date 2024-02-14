package eu.clarin.sru.fcs.tester.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;

import eu.clarin.sru.fcs.tester.FCSEndpointValidationRequest;
import eu.clarin.sru.fcs.tester.FCSEndpointValidationResponse;

@PageTitle("FCS SRU Endpoint Conformance Tester")
@Route
@PreserveOnRefresh
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
        mainContent.setWidth("100%");
        mainContent.setHeight("100%");
        mainContent.setPadding(false);
        mainContent.setMargin(false);
        mainContent.getStyle().set("flex-grow", "1");

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
        mainContent.add(new ResultsView(result));
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
        txtEndpointURL.focus();
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
        vlButtons.addClassName(Padding.XSMALL);
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

}
