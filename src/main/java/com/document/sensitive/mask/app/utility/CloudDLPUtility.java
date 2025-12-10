package com.document.sensitive.mask.app.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import com.document.sensitive.mask.app.constants.ApplicationConstants;
import com.google.api.gax.rpc.ApiException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.cloud.dlp.v2.DlpServiceSettings;
import com.google.privacy.dlp.v2.ContentItem;
import com.google.privacy.dlp.v2.CustomInfoType;
import com.google.privacy.dlp.v2.CustomInfoType.Regex;
import com.google.privacy.dlp.v2.Finding;
import com.google.privacy.dlp.v2.InfoType;
import com.google.privacy.dlp.v2.InspectConfig;
import com.google.privacy.dlp.v2.InspectContentRequest;
import com.google.privacy.dlp.v2.InspectContentResponse;

/**
 * @author lalamanil
 **/
public class CloudDLPUtility {

	private static final Logger LOGGER = Logger.getLogger(CloudDLPUtility.class.getName());

	private static DlpServiceClient dlpServiceClient;

	static {
		InputStream inputStream = CloudDLPUtility.class.getClassLoader().getResourceAsStream("AI-ServiceAccount.json");
		if (null == inputStream) {
			LOGGER.info(
					"inputstream for service account is null. Please check AI-ServiceAccount.json is present in src/main/resources");
		} else {
			try {
				GoogleCredentials googleCredentials = GoogleCredentials.fromStream(inputStream)
						.createScoped("https://www.googleapis.com/auth/cloud-platform");
				DlpServiceSettings dlpServiceSettings = DlpServiceSettings.newBuilder()
						.setCredentialsProvider(() -> googleCredentials).build();
				dlpServiceClient = DlpServiceClient.create(dlpServiceSettings);
			} catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();
			}

		}
	}

	public static List<Finding> inspectWithDlp(String rawString) {
		List<Finding> findings = new ArrayList();
		if (null == dlpServiceClient) {
			LOGGER.info("dlpServiceClient is null or empty. Please check application logs");
		} else {

			List<InfoType> infoTypes = new ArrayList<InfoType>();

			ApplicationConstants.globalInfoTypes.forEach(infotype -> {
				infoTypes.add(InfoType.newBuilder().setName(infotype).build());
			});

			CustomInfoType uscisReceiptType = CustomInfoType.newBuilder()
					.setInfoType(InfoType.newBuilder().setName("USCIS_RECEIPT_NUMBER"))
					.setRegex(Regex.newBuilder().setPattern("[A-Z]{3}[0-9]{10}")).build();

			CustomInfoType accountNumber = CustomInfoType.newBuilder()
					.setInfoType(InfoType.newBuilder().setName("Account Number"))
					.setRegex(Regex.newBuilder()
							.setPattern("((Account #|Account number:)\\s*)((\\d{4}\\s*){2}\\d{4}|\\d+)")
							.addGroupIndexes(3))
					.build();

			InspectConfig inspectConfig = InspectConfig.newBuilder().addAllInfoTypes(infoTypes)
					.addCustomInfoTypes(uscisReceiptType).addCustomInfoTypes(accountNumber).setIncludeQuote(true)
					.build();

			ContentItem contentItem = ContentItem.newBuilder().setValue(rawString).build();

			String parent = String.format("projects/%s/locations/global", ApplicationConstants.PROJECT_ID);

			InspectContentRequest request = InspectContentRequest.newBuilder().setParent(parent)
					.setInspectConfig(inspectConfig).setItem(contentItem).build();

			try {
				InspectContentResponse response = dlpServiceClient.inspectContent(request);
				if (null != response && null != response.getResult()) {
					findings = response.getResult().getFindingsList();
				}
			} catch (ApiException e) {
				// TODO: handle exception
				e.printStackTrace();
			}

		}

		return findings;

	}

}
