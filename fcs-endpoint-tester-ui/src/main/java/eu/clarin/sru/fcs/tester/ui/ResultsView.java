package eu.clarin.sru.fcs.tester.ui;

import java.util.List;

import org.apache.logging.log4j.core.LogEvent;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;

import eu.clarin.sru.fcs.tester.FCSEndpointValidationRequest;
import eu.clarin.sru.fcs.tester.FCSEndpointValidationResponse;
import eu.clarin.sru.fcs.tester.FCSTestResult;

public class ResultsView extends VerticalLayout {

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
