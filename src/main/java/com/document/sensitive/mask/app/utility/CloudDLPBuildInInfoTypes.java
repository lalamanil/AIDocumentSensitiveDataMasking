package com.document.sensitive.mask.app.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.document.sensitive.mask.app.constants.ApplicationConstants;
import com.google.api.gax.rpc.ApiException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.cloud.dlp.v2.DlpServiceSettings;
import com.google.privacy.dlp.v2.ListInfoTypesRequest;
import com.google.privacy.dlp.v2.ListInfoTypesResponse;
import com.google.privacy.dlp.v2.LocationName;

/**
 * @author lalamanil
 **/

public class CloudDLPBuildInInfoTypes {

	private static final Logger LOGGER = Logger.getLogger(CloudDLPBuildInInfoTypes.class.getName());

	private static DlpServiceClient dlpServiceClient;

	static {
		InputStream inputStream = CloudDLPBuildInInfoTypes.class.getClassLoader()
				.getResourceAsStream("AI-ServiceAccount.json");
		if (null == inputStream) {
			LOGGER.info("Please check Service account file valid or not");
		} else {
			try {
				GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream)
						.createScoped("https://www.googleapis.com/auth/cloud-platform");
				DlpServiceSettings dlpServiceSettings = DlpServiceSettings.newBuilder()
						.setCredentialsProvider(() -> credentials).build();
				dlpServiceClient = DlpServiceClient.create(dlpServiceSettings);
			} catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();
			}

		}

	}

	public static void getSupportedBuildInInfoTypesForLocation(String locationId) {
		if (null == dlpServiceClient) {
			LOGGER.info("DlpServiceClient is not initialized.");
			return;
		}
		String parent = LocationName.of(ApplicationConstants.PROJECT_ID, locationId).toString();
		System.out.println("parent is:" + parent);
		ListInfoTypesRequest listInfoTypesRequest = ListInfoTypesRequest.newBuilder().setParent(parent)
				.setLanguageCode("en").build();
		try {
			ListInfoTypesResponse response = dlpServiceClient.listInfoTypes(listInfoTypesRequest);

			List<String> infotypes = response.getInfoTypesList().stream().map(d -> d.getName())
					.collect(Collectors.toList());
			if (null != infotypes && !infotypes.isEmpty()) {

				infotypes.stream().forEach(infotype -> {

					System.out.println(infotype);
				});

				printArrayList(infotypes);
			}
		} catch (ApiException e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}

	private static void printArrayList(List<String> infoTypeList) {
		StringBuilder builder = new StringBuilder();
		builder.append("Arrays.asList(");
		for (int i = 0; i < infoTypeList.size(); i++) {
			if (i == 0) {
				builder.append("\"" + infoTypeList.get(i) + "\"");
			} else {
				builder.append(",\"" + infoTypeList.get(i) + "\"");
			}
		}
		builder.append(");");
		System.out.println(builder.toString());
	}

	public static void main(String[] args) {

		getSupportedBuildInInfoTypesForLocation("global");
	}
}
