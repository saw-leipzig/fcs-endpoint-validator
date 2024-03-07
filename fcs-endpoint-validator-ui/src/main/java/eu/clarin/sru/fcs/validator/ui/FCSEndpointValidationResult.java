package eu.clarin.sru.fcs.validator.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.validator.FCSEndpointValidationRequest;
import eu.clarin.sru.fcs.validator.FCSEndpointValidationResponse;

public class FCSEndpointValidationResult implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(FCSEndpointValidationResult.class);

    private static final long serialVersionUID = 2024_03_07L;

    // test request and results
    private final FCSEndpointValidationRequest request;
    private final FCSEndpointValidationResponse response;

    // origin of test request and when it was requested
    private final String ipAddress;
    private final Instant datetime;

    // custom meta data from user
    private String title;
    private String description;

    // for programm only (state, not required to save)
    private transient boolean isSaved = false;
    private transient String saveId = null;
    private transient int saveSize = -1;

    // ----------------------------------------------------------------------

    public FCSEndpointValidationResult(FCSEndpointValidationRequest request, FCSEndpointValidationResponse response,
            String ipAddress, Instant datetime) {
        if (response == null) {
            throw new NullPointerException("response == null");
        }

        this.request = request;
        this.response = response;
        this.ipAddress = ipAddress;
        this.datetime = datetime;
    }

    public FCSEndpointValidationResult(FCSEndpointValidationResponse response, String ipAddress, Instant datetime) {
        this(null, response, ipAddress, datetime);
    }

    // ----------------------------------------------------------------------

    public FCSEndpointValidationRequest getRequest() {
        if (request != null) {
            return request;
        } else {
            return response.getRequest();
        }
    }

    public FCSEndpointValidationResponse getResponse() {
        return response;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getDatetime() {
        return datetime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // ----------------------------------------------------------------------

    public boolean isSaved() {
        return isSaved;
    }

    protected void setSaved(boolean isSaved) {
        this.isSaved = isSaved;
    }

    public String getSaveId() {
        return saveId;
    }

    protected void setSaveId(String saveId) {
        this.saveId = saveId;
    }

    public int getSaveSize() {
        return saveSize;
    }

    protected void setSaveSize(int saveSize) {
        this.saveSize = saveSize;
    }

    // ----------------------------------------------------------------------

    public static byte[] serialize(FCSEndpointValidationResult result, String resultId) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(result);
        } catch (IOException e) {
            logger.error("Error serializing endpoint validation results", e);
            return null;
        }
        byte[] bytes = baos.toByteArray();

        // side-effect, update save properties
        result.setSaved(true);
        result.setSaveId(resultId);
        result.setSaveSize(bytes.length);

        return bytes;
    }

    public static byte[] serialize(FCSEndpointValidationResult result) {
        return serialize(result, null);
    }

    public static FCSEndpointValidationResult deserialize(final byte[] bytes, final String resultId) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            FCSEndpointValidationResult result = (FCSEndpointValidationResult) ois.readObject();

            result.setSaved(true);
            result.setSaveId(resultId);
            result.setSaveSize(bytes.length);

            return result;
        } catch (InvalidClassException e) {
            logger.error("Error deserializing endpoint validation results (signature changed!)", e);
        } catch (ClassNotFoundException | IOException e) {
            logger.error("Error deserializing endpoint validation results", e);
        }

        return null;
    }

    public static FCSEndpointValidationResult deserialize(final byte[] bytes) {
        return deserialize(bytes, null);
    }
}