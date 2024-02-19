package eu.clarin.sru.fcs.validator.ui;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;

import eu.clarin.sru.fcs.validator.FCSEndpointValidatorProgressListener;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationRequest;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationResponse;
import eu.clarin.sru.fcs.validator.FCSTestResult;

public class ProgressView extends VerticalLayout implements FCSEndpointValidatorProgressListener {
    protected static final Logger logger = LoggerFactory.getLogger(ProgressView.class);

    final UI ui;

    ProgressBar prgbr;
    Div prgList;

    long maximumNumberOfTest = -1;
    long numberOfTestsRun = 0;

    public ProgressView() {
        ui = UI.getCurrent();

        addClassName(Gap.XSMALL);
        setWidth("100%");
        // setMaxWidth("35rem");
        setHeight("100%");
        getStyle()
                .set("flex-grow", "1")
                .set("margin-block", "0")
                .set("paddding-block", "0");

        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        // --------------------------------------------------------

        add(createProgressBar());
        add(createProgressMessageList());
    }

    // ----------------------------------------------------------------------

    private List<Component> createProgressBar() {
        prgbr = new ProgressBar();
        prgbr.setIndeterminate(true);

        NativeLabel prgbrLbl = new NativeLabel("Running FCS Endpoint Validation ...");
        prgbrLbl.setId("pblbl");
        prgbrLbl.addClassName(LumoUtility.TextColor.SECONDARY);

        // Span prgbrSubLbl = new Span("Process can take upwards of 10 minutes");
        // prgbrSubLbl.setId("sublbl");
        // prgbrSubLbl.addClassNames(LumoUtility.TextColor.SECONDARY,
        // LumoUtility.FontSize.XSMALL);

        // Associates the labels with the bar programmatically, for screen readers:
        prgbr.getElement().setAttribute("aria-labelledby", "pblbl");
        // prgbr.getElement().setAttribute("aria-describedby", "sublbl");

        return List.of(prgbrLbl, prgbr);
    }

    private final int MAX_MESSAGES = 10;

    private Component createProgressMessageList() {
        prgList = new Div();
        prgList.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.AlignItems.CENTER,
                LumoUtility.FontSize.SMALL, LumoUtility.TextColor.TERTIARY, LumoUtility.Overflow.HIDDEN);
        prgList.setHeight(String.format("calc( %sem * var(--lumo-line-height-m))", MAX_MESSAGES));

        return prgList;
    }

    // ----------------------------------------------------------------------

    @Override
    public void setNumberOfTests(long numTests) {
        maximumNumberOfTest = numTests;

        ui.access(() -> {
            prgbr.setMin(0);
            prgbr.setMax(maximumNumberOfTest);
            prgbr.setValue(numberOfTestsRun);
            prgbr.setIndeterminate(false);
        });
    }

    public void incrementProgress() {
        numberOfTestsRun++;
        ui.access(() -> {
            if (maximumNumberOfTest >= 0) {
                prgbr.setValue(numberOfTestsRun);
            }
        });
    }

    private void addMessage(Component message) {
        ui.access(() -> {
            prgList.add(message);

            // fade out older messages
            long max = prgList.getChildren().count();
            AtomicInteger index = new AtomicInteger(0);
            prgList.getChildren().forEach(c -> {
                int i = index.incrementAndGet();
                if (i <= max - MAX_MESSAGES) {
                    c.addClassName("hide-fadeaway");
                    // c.setVisible(false);
                }
            });

        });
    }

    @Override
    public void onProgressMessage(String message) {
        addMessage(new Span(message));
    }

    @Override
    public void onStarted(FCSEndpointValidationRequest request) {
        String profileName = "Auto-detect";
        if (request.getFCSTestProfile() != null) {
            profileName = request.getFCSTestProfile().toDisplayString();
        }

        Span msg = new Span();
        msg.add("Start FCS Endpoint validation of ");
        msg.add(new Anchor(request.getBaseURI(), request.getBaseURI(), AnchorTarget.BLANK));
        msg.add(" with test profile ");
        Span txtProfile = new Span(profileName);
        txtProfile.addClassName(LumoUtility.TextColor.PRIMARY);
        msg.add(txtProfile);
        msg.add(".");

        addMessage(msg);
    }

    @Override
    public void onFinished(FCSEndpointValidationResponse response) {
        addMessage(new Span("Finished FCS Endpoint validation."));
    }

    @Override
    public void onTestStarted(String testId, String testName) {
        Span msg = new Span();
        msg.add("Started test ");
        Span txtTestName = new Span(testName);
        txtTestName.addClassName(LumoUtility.TextColor.PRIMARY);
        msg.add(txtTestName);
        msg.add(" ...");

        addMessage(msg);
        incrementProgress();
    }

    /*
    // @formatter:off
    @Override
    public void onTestFinished(String testId, String testName, FCSTestResult result) {
        Span msg = new Span();
        msg.add("Finished test ");
        Span txtTestName = new Span(testName);
        txtTestName.addClassName(LumoUtility.TextColor.PRIMARY);
        msg.add(txtTestName);
        msg.add(" with status ");
        Span txtTestStatus = new Span(result.getStatus().toString());
        txtTestStatus.addClassName(LumoUtility.TextColor.SECONDARY);
        msg.add(txtTestStatus);
        msg.add(".");

        addMessage(msg);
    }
    // @formatter:on
    */

}
