package eu.clarin.sru.fcs.validator.ui;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;

import eu.clarin.sru.fcs.validator.FCSEndpointValidatorProgressListener;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationRequest;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationResponse;
import eu.clarin.sru.fcs.validator.FCSTestConstants;
import eu.clarin.sru.fcs.validator.FCSTestProfile;

@PageTitle("FCS SRU Endpoint Validator")
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

    Dialog dlgAdditionalConfigurations;
    Select<FCSTestProfile> selTestProfile;
    Checkbox chkStrictMode;
    Checkbox chkProbeRequest;
    Select<Integer> selIndentResponse;
    Select<Integer> selConnectTimeout;
    Select<Integer> selSocketTimeout;

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
        add(createAdditionConfigurationsDialog());
        add(mainContent);
        add(createFooter());

        // --------------------------------------------------------
        // event handlers

        btnStart.addClickShortcut(Key.ENTER);
        btnStart.addClickListener(event -> {
            // input field validation should happen automatically

            // build FCS Validation Request
            final FCSEndpointValidationRequest request = new FCSEndpointValidationRequest();
            // required inputs
            request.setBaseURI(txtEndpointURL.getValue().strip());
            request.setUserSearchTerm(txtSearchTerm.getValue());
            // and additional configurations
            request.setFCSTestProfile(selTestProfile.getValue());
            request.setStrictMode(chkStrictMode.getValue());
            request.setPerformProbeRequest(chkProbeRequest.getValue());
            request.setIndentResponse(selIndentResponse.getValue());
            request.setConnectTimeout(selConnectTimeout.getValue());
            request.setSocketTimeout(selSocketTimeout.getValue());

            // NOTE: this is a bit weird/side-effecty
            // we need to create a view but the listener is connected to it
            FCSEndpointValidatorProgressListener progressListener = setMainContentInProgress();
            request.setProgressListener(progressListener);

            final UI ui = UI.getCurrent();
            try {
                FCSEndpointValidatorService.getInstance().evalute(request).thenAccept((response) -> {
                    ui.access(() -> {
                        // re-enable input for user
                        setInputEnabled(true);
                        // render results
                        setMainContentResults(response);
                    });
                }).exceptionally(ex -> {
                    logger.error("Exception handler", ex);
                    final Throwable realEx = ex.getCause();
                    ui.access(() -> {
                        // re-enable input for user
                        setInputEnabled(true);
                        // show error message
                        setMainContentError("An error occurred!", realEx);
                    });
                    return null;
                });
            } catch (Exception ex) {
                logger.error("Some unexpected error occured?", ex);
                // re-enable input for user
                setInputEnabled(true);
                // show error message
                setMainContentError("An internal error occurred!", ex);
            }
        });

        btnConfig.addClickListener(event -> {
            dlgAdditionalConfigurations.open();
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

    public FCSEndpointValidatorProgressListener setMainContentInProgress() {
        mainContent.removeAll();

        ProgressView progressView = new ProgressView();
        mainContent.add(progressView);
        mainContent.setAlignSelf(Alignment.CENTER, progressView);

        // our progress view is also the listener
        return progressView;
    }

    public void setMainContentResults(FCSEndpointValidationResponse result) {
        mainContent.removeAll();
        mainContent.add(new ResultsView(result));
    }

    public void setMainContentError(String title, Throwable t) {
        mainContent.removeAll();
        mainContent.add(createShowErrorContent(title, t));
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
        lblTitle.setText("FCS SRU Endpoint Validator");
        lblTitle.setWidth("max-content");
        lblTitle.getStyle().set("font-size", "var(--lumo-font-size-xxl)");
        titleRow.add(lblTitle);

        Image imgLogo = new Image("themes/fcs-endpoint-validator/images/logo-saw.png", "Logo SAW");
        imgLogo.setHeight("35px"); // based on H1; var(--lumo-icon-size-l) ?
        imgLogo.addClassName(LumoUtility.Margin.Left.AUTO);
        titleRow.add(imgLogo);

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
        linkEmail.setTarget(AnchorTarget.BLANK);
        txtFooter.setWidth("max-content");
        txtFooter.add(linkEmail);

        txtFooter.add(".");

        footerRow.add(txtFooter);

        return footerRow;
    }

    // ----------------------------------------------------------------------

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

    public Component createAdditionConfigurationsDialog() {
        dlgAdditionalConfigurations = new Dialog();
        dlgAdditionalConfigurations.setHeaderTitle("More Configurations");

        Button closeButton = new Button(new Icon("lumo", "cross"), (e) -> dlgAdditionalConfigurations.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dlgAdditionalConfigurations.getHeader().add(closeButton);

        FormLayout flAdditionInputs = new FormLayout();
        flAdditionInputs.setMaxWidth("20rem");
        flAdditionInputs.setResponsiveSteps(new ResponsiveStep("0", 1));

        selTestProfile = new Select<>();
        selTestProfile.setLabel("FCS Test Profile");
        selTestProfile.addClassNames(LumoUtility.Padding.Top.NONE, LumoUtility.Padding.Bottom.MEDIUM);
        selTestProfile.setEmptySelectionAllowed(true);
        selTestProfile.setEmptySelectionCaption("Auto detect");
        selTestProfile.setItemLabelGenerator(profile -> (profile == null) ? "Auto detect" : profile.toDisplayString());
        selTestProfile.setItems(FCSTestProfile.CLARIN_FCS_2_0, FCSTestProfile.CLARIN_FCS_1_0,
                FCSTestProfile.CLARIN_FCS_LEGACY, FCSTestProfile.LEX_FCS);
        selTestProfile.addComponentAtIndex(1, new Hr());
        selTestProfile.addComponents(FCSTestProfile.CLARIN_FCS_2_0, new Hr());
        selTestProfile.addComponents(FCSTestProfile.CLARIN_FCS_LEGACY, new Hr());
        selTestProfile.setRenderer(new ComponentRenderer<>(profile -> {
            FlexLayout wrapper = new FlexLayout();
            wrapper.setAlignItems(Alignment.CENTER);
            wrapper.addClassName(Gap.SMALL);

            Icon icon;
            String label;
            switch (profile) {
                case CLARIN_FCS_2_0:
                    label = "Latest FCS version";
                    icon = VaadinIcon.CHECK.create();
                    icon.getElement().getThemeList().add("badge success");
                    break;
                case LEX_FCS:
                    label = "Experimental FCS version";
                    icon = VaadinIcon.TOOLS.create();
                    icon.getElement().getThemeList().add("badge contrast");
                    break;
                default:
                    label = "Legacy or unknown FCS version";
                    icon = VaadinIcon.EXCLAMATION.create();
                    icon.getElement().getThemeList().add("badge");
                    break;
            }
            icon.getStyle().set("padding", "var(--lumo-space-xs");
            icon.getElement().setAttribute("aria-label", label);
            icon.getElement().setAttribute("title", label);
            wrapper.add(icon);

            wrapper.add(new Span(profile.toDisplayString()));

            return wrapper;
        }));
        flAdditionInputs.add(selTestProfile);

        chkStrictMode = new Checkbox();
        chkStrictMode.setLabel("Perform checks in strict mode");
        chkStrictMode.setValue(true);
        flAdditionInputs.add(chkStrictMode);

        chkProbeRequest = new Checkbox();
        chkProbeRequest.setLabel("Perform HTTP HEAD probe request");
        chkProbeRequest.setValue(true);
        flAdditionInputs.add(chkProbeRequest);

        selIndentResponse = new Select<>();
        selIndentResponse.setLabel("Optional response indentation");
        selIndentResponse.setTooltipText(
                "Optional response indentation. If set to -1/'off' then the parameter '"
                        + FCSTestConstants.X_INDENT_RESPONSE + "' is not sent."
                        + " This parameter will be set for all test requests!");
        selIndentResponse.setItemLabelGenerator(new ItemLabelGenerator<>() {
            @Override
            public String apply(Integer indent) {
                if (indent < 0) {
                    return "off, unsent";
                }
                if (indent == 0) {
                    return "off, sent (0)";
                }
                return String.valueOf(indent);
            }
        });
        selIndentResponse.setItems(new Integer[] { -1, 0, 1, 2, 4 });
        selIndentResponse.setValue(-1);
        flAdditionInputs.add(selIndentResponse);

        Integer[] timeouts = new Integer[] { 5_000, 10_000, 15_000, 30_000, 60_000, 120_000, 180_000, 300_000 };
        ItemLabelGenerator<Integer> timeoutString = new ItemLabelGenerator<>() {
            @Override
            public String apply(Integer timeout) {
                if (timeout <= 0) {
                    return "No timeout";
                } else if (timeout < 1000) {
                    return String.format("%s milliseconds", timeout);
                } else if (timeout >= 1000 && timeout < 60_000) {
                    return String.format("%s second%s", Math.round(timeout / 1000),
                            (Math.round(timeout / 1000) == 1) ? "" : "s");
                } else {
                    return String.format("%s minute%s", Math.round(timeout / 1000 / 60),
                            (Math.round(timeout / 1000 / 60) == 1) ? "" : "s");
                }
            }
        };

        selConnectTimeout = new Select<>();
        selConnectTimeout.setLabel("Connection Timeout");
        selConnectTimeout.setItemLabelGenerator(timeoutString);
        selConnectTimeout.setItems(timeouts);
        selConnectTimeout.setValue(15_000);
        flAdditionInputs.add(selConnectTimeout);

        selSocketTimeout = new Select<>();
        selSocketTimeout.setLabel("Socket Timeout");
        selSocketTimeout.setItemLabelGenerator(timeoutString);
        selSocketTimeout.setItems(timeouts);
        selSocketTimeout.setValue(30_000);
        flAdditionInputs.add(selSocketTimeout);

        dlgAdditionalConfigurations.add(flAdditionInputs);

        return dlgAdditionalConfigurations;
    }

    // ----------------------------------------------------------------------

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

    public Component createShowErrorContent(String title, Throwable t) {
        VerticalLayout vlError = new VerticalLayout();
        vlError.setWidth("100%");
        vlError.setHeight("100%");
        vlError.getStyle().set("flex-grow", "1");
        vlError.setJustifyContentMode(JustifyContentMode.CENTER);
        vlError.setAlignItems(Alignment.CENTER);

        H2 txtErrorTitle = new H2(title);
        vlError.add(txtErrorTitle);

        Pre txtErrorStacktrace = new Pre();
        txtErrorStacktrace.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.AlignItems.START, LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY,
                LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.Margin.Top.XSMALL);

        // color the exception title/message
        Span exceptionTitle = new Span(t.toString());
        exceptionTitle.addClassName(LumoUtility.TextColor.PRIMARY);
        txtErrorStacktrace.add(exceptionTitle);

        // let java format us the message ...
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(baos)) {
            t.printStackTrace(pw);
        }
        String stackTrace = baos.toString();
        txtErrorStacktrace.add(stackTrace.substring(stackTrace.indexOf("\n") + 1));

        vlError.add(txtErrorStacktrace);

        return vlError;
    }

}
