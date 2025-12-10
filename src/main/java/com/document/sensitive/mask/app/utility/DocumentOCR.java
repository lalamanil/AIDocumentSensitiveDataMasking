package com.document.sensitive.mask.app.utility;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import com.document.sensitive.mask.app.constants.ApplicationConstants;
import com.document.sensitive.mask.app.model.OCRResponseModel;
import com.document.sensitive.mask.app.model.TokenModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.Document.Page;
import com.google.cloud.documentai.v1.Document.Page.Layout;
import com.google.cloud.documentai.v1.Document.Page.Token;
import com.google.cloud.documentai.v1.Document.TextAnchor;
import com.google.cloud.documentai.v1.Document.TextAnchor.TextSegment;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import com.google.cloud.documentai.v1.NormalizedVertex;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.privacy.dlp.v2.Finding;
import com.google.protobuf.ByteString;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.exceptions.PdfException;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

/**
 * @author lalamanil
 **/
public class DocumentOCR {
	private static final Logger LOGGER = Logger.getLogger(DocumentOCR.class.getName());
	private static DocumentProcessorServiceClient documentProcessorServiceClient;
	static {
		InputStream inputStream = DocumentOCR.class.getClassLoader().getResourceAsStream("AI-ServiceAccount.json");
		if (null == inputStream) {
			LOGGER.info(
					"inputstream for Service account is null or empty. Please check service account is in src/main/resources");
		} else {
			try {
				GoogleCredentials googleCredentials = GoogleCredentials.fromStream(inputStream)
						.createScoped("https://www.googleapis.com/auth/cloud-platform");
				String endpoint = String.format("%s-documentai.googleapis.com:443",
						ApplicationConstants.DOCUMENT_AI_PROCESSORS_LOCATION);
				DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder()
						.setCredentialsProvider(() -> googleCredentials).setEndpoint(endpoint).build();
				documentProcessorServiceClient = DocumentProcessorServiceClient.create(settings);
			} catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();

			}

		}
	}

	public static OCRResponseModel processDocument(byte[] filedata, String mimeType) {

		Map<Integer, List<TokenModel>> tokenListMap = new HashMap<Integer, List<TokenModel>>();
		OCRResponseModel ocrResponseModel = new OCRResponseModel();
		ocrResponseModel.setPageTokenMap(tokenListMap);

		if (null == documentProcessorServiceClient) {
			LOGGER.info("documentProcessorServiceClient is null. Please check application logs");
		} else {

			if (null == filedata) {
				LOGGER.info("filedata is null or empty");
			} else {
				String name = String.format("projects/%s/locations/%s/processors/%s", ApplicationConstants.PROJECT_ID,
						ApplicationConstants.DOCUMENT_AI_PROCESSORS_LOCATION, ApplicationConstants.PROCESSOR_ID);
				ByteString content = ByteString.copyFrom(filedata);
				RawDocument rawDocument = RawDocument.newBuilder().setContent(content).setMimeType(mimeType).build();
				ProcessRequest processRequest = ProcessRequest.newBuilder().setName(name).setRawDocument(rawDocument)
						.build();
				ProcessResponse processResponse = documentProcessorServiceClient.processDocument(processRequest);
				Document document = processResponse.getDocument();
				String rawText = document.getText();
				ocrResponseModel.setRawText(rawText);
				System.out.println("rawText:" + ocrResponseModel.getRawText());
				List<Page> pageList = document.getPagesList();
				if (null == pageList || pageList.isEmpty()) {
					LOGGER.info("pagelist received is null or empty for a document");
				} else {
					for (int p = 0; p < pageList.size(); p++) {

						Page page = pageList.get(p);

						List<Token> tokenList = page.getTokensList();

						if (null == tokenList || tokenList.isEmpty())
							continue;

						List<TokenModel> pageTokenlist = new ArrayList<TokenModel>();

						float pageWidth = page.getDimension().getWidth();
						float pageHeight = page.getDimension().getHeight();

						for (Token token : tokenList) {
							Layout layout = token.getLayout();
							if (null == layout)
								continue;
							String tokenText = getText(layout.getTextAnchor(), rawText);
							int startindex = 0, endIndex = 0;
							if (layout.getTextAnchor().getTextSegmentsCount() > 0) {
								TextSegment seg = layout.getTextAnchor().getTextSegments(0);
								startindex = (int) seg.getStartIndex();
								endIndex = (int) seg.getEndIndex();
							}
							// Normalized vertices
							List<NormalizedVertex> normalizedVertexs = layout.getBoundingPoly()
									.getNormalizedVerticesList();
							if (null == normalizedVertexs || normalizedVertexs.isEmpty())
								continue;
							float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = 0, maxY = 0;
							for (NormalizedVertex nv : normalizedVertexs) {
								float x = nv.getX() * pageWidth;
								float y = nv.getY() * pageHeight;
								minX = Math.min(minX, x);
								minY = Math.min(minY, y);
								maxX = Math.max(maxX, x);
								maxY = Math.max(maxY, y);
							}

							pageTokenlist.add(new TokenModel(p + 1, tokenText, startindex, endIndex, minX, minY, maxX,
									maxY, pageWidth, pageHeight));
						}
						if (!pageTokenlist.isEmpty()) {
							tokenListMap.put(p + 1, pageTokenlist);
						}
					}
					System.out.println(ocrResponseModel.getPageTokenMap().size());
				}
			}
		}
		return ocrResponseModel;
	}

	private static String getText(TextAnchor textAnchor, String rawtext) {
		StringBuilder sb = new StringBuilder();
		if (null != textAnchor) {
			List<TextSegment> textSegments = textAnchor.getTextSegmentsList();
			if (null != textSegments && !textSegments.isEmpty()) {
				for (TextSegment textSegment : textSegments) {
					int start = (int) textSegment.getStartIndex();
					int end = (int) textSegment.getEndIndex();

					if (end > rawtext.length()) {

						end = rawtext.length();
					}
					sb.append(rawtext.subSequence(start, end));

				}

			}

		}
		return sb.toString();
	}

	private static int getCharacterIndex(int byteIndex, String rawText) {
		if (byteIndex == 0)
			return 0;
		byte[] utf8Bytes = rawText.getBytes(StandardCharsets.UTF_8);
		int targetlenght = Math.min(byteIndex, utf8Bytes.length);
		String partialString = new String(utf8Bytes, 0, targetlenght, StandardCharsets.UTF_8);
		return partialString.length();
	}

	private static List<TokenModel> getTokenModelOfFindings(byte[] filebytes, String mimeType) {
		List<TokenModel> tokenToMask = new ArrayList<TokenModel>();
		OCRResponseModel ocrResponseModel = processDocument(filebytes, mimeType);
		String rawtext = ocrResponseModel.getRawText();
		if (null == rawtext) {
			LOGGER.info("rawtext from document OCR is null");
		} else {
			List<Finding> findings = CloudDLPUtility.inspectWithDlp(rawtext);
			if (null != findings && !findings.isEmpty()) {
				for (Finding f : findings) {
					if (!f.hasLocation() || !f.getLocation().hasByteRange())
						continue;
					long start = f.getLocation().getByteRange().getStart();
					long end = f.getLocation().getByteRange().getEnd();
					int charStart = getCharacterIndex((int) start, rawtext);
					int charEnd = getCharacterIndex((int) end, rawtext);
					System.out.println(f.getInfoType().getName() + ":" + charStart + ":" + charEnd);
					System.out.println(rawtext.substring((int) charStart, (int) charEnd));
					for (List<TokenModel> pageTokens : ocrResponseModel.getPageTokenMap().values()) {
						for (TokenModel token : pageTokens) {
							int tStart = token.getStartIndex();
							int tend = token.getEndIndex();

							if (tend > charStart && tStart < charEnd) {
								tokenToMask.add(token);
							}

						}
					}

				}

			} else {
				LOGGER.info("findings is null or empty");
			}
		}
		return tokenToMask;
	}

	public static void printTokens(List<TokenModel> tokenList) {
		ObjectMapper mapper = new ObjectMapper();
		tokenList.forEach(tm -> {
			try {
				System.out.println(mapper.writeValueAsString(tm));
			} catch (JsonProcessingException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		});
	}

	public static void sortTokenModelsByPageNumbers(List<TokenModel> tokenList) {
		Collections.sort(tokenList, new Comparator<TokenModel>() {
			@Override
			public int compare(TokenModel o1, TokenModel o2) {
				// TODO Auto-generated method stub
				return o1.getPageNumber() - o2.getPageNumber();
			}
		});
	}

	public static Map<Integer, List<TokenModel>> getPageTokenModelMap(List<TokenModel> tokenList) {
		Map<Integer, List<TokenModel>> pageTokenListMap = new HashMap<Integer, List<TokenModel>>();
		if (null != tokenList && !tokenList.isEmpty()) {
			pageTokenListMap = tokenList.stream()
					.collect(Collectors.groupingBy(tokenmodel -> tokenmodel.getPageNumber()));
		}

		return pageTokenListMap;
	}

	public static byte[] maskImageTokens(byte[] inputImageBytes, Map<Integer, List<TokenModel>> pageTokenModelMap,
			String formatName) {

		if (null == inputImageBytes || null == pageTokenModelMap || pageTokenModelMap.isEmpty()) {
			return null;
		}
		BufferedImage image = null;
		ByteArrayInputStream inputStream = null;
		try {
			inputStream = new ByteArrayInputStream(inputImageBytes);
			image = ImageIO.read(inputStream);
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		} finally {
			if (null != inputStream) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// TODO: handle exception
					e.printStackTrace();
				}

			}
		}
		if (null == image) {

			LOGGER.info("Could not read the image from bytes");
			return null;
		}
		Graphics2D graphics = image.createGraphics();
		// Mask color definition
		Color maskColor = new Color(204, 204, 204);
		graphics.setColor(maskColor);
		float padding = 1.0f;
		int actualImageWidth = image.getWidth();
		int actualImageHeight = image.getHeight(); // java image access is Top-Down

		Set<Map.Entry<Integer, List<TokenModel>>> pageTokenEntrySet = pageTokenModelMap.entrySet();

		for (Map.Entry<Integer, List<TokenModel>> entry : pageTokenEntrySet) {

			List<TokenModel> tokensToMask = entry.getValue();

			if (tokensToMask.isEmpty())
				continue;

			float docAIWidth = tokensToMask.get(0).getPageWidth();
			float docAIHeight = tokensToMask.get(0).getPageHeight();

			int rotationtouse = getinferrenceRotation(actualImageHeight, actualImageWidth, docAIHeight, docAIWidth, 0);

			System.out.println("Image rotation Inferred:" + rotationtouse);

			for (TokenModel token : tokensToMask) {

				float llx, lly, width, height;
				float xScaleFactor, yScaleFactor;

				if (rotationtouse == 90) {
					// 90 Degree rotation (cw) Transforming (x,y) -> (H_DocAI-y, x)

					float rotatedMinX = docAIHeight - token.getMaxY();
					float rotatedMinY = token.getMinX();

					// Scaling Factors
					xScaleFactor = (float) actualImageWidth / docAIHeight;
					yScaleFactor = (float) actualImageHeight / docAIWidth;

					// applying scaling
					llx = rotatedMinX * xScaleFactor;
					lly = rotatedMinY * yScaleFactor;

					width = (token.getMaxY() - token.getMinY()) * xScaleFactor;
					height = (token.getMaxX() - token.getMinX()) * yScaleFactor;
				} else {
					// 0 degree rotation

					xScaleFactor = (float) actualImageWidth / docAIWidth;
					yScaleFactor = (float) actualImageHeight / docAIHeight;

					llx = token.getMinX() * xScaleFactor;
					lly = token.getMinY() * yScaleFactor;

					width = (token.getMaxX() - token.getMinX()) * xScaleFactor;
					height = (token.getMaxY() - token.getMinY()) * yScaleFactor;

				}

				int drawX = Math.round(llx - (padding / 2));
				int drawY = Math.round(lly - (padding / 2));
				int drawWidth = Math.round(width + padding);
				int drawHeight = Math.round(height + padding);

				graphics.fillRect(drawX, drawY, drawWidth, drawHeight);

			}

		}
		graphics.dispose();
		java.io.ByteArrayOutputStream outputStream = null;
		try {
			outputStream = new java.io.ByteArrayOutputStream();

			if ("jpg".equalsIgnoreCase(formatName) || "jpeg".equalsIgnoreCase(formatName)) {

				if (image.getType() == 0) {

					BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(),
							BufferedImage.TYPE_INT_RGB);
					Graphics2D g2d = newImage.createGraphics();
					g2d.drawImage(image, 0, 0, null);
					g2d.dispose();
					image = newImage;

				}

			}

			ImageIO.write(image, formatName, outputStream);
			return outputStream.toByteArray();

		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		} finally {
			if (null != outputStream) {

				try {
					outputStream.close();
				} catch (IOException e) {
					// TODO: handle exception
					e.printStackTrace();
				}

			}
		}

	}

	public static byte[] maskPdfTokenks(byte[] inputPdfBytes, Map<Integer, List<TokenModel>> pageTokenModelMap) {

		if (null == inputPdfBytes || null == pageTokenModelMap || pageTokenModelMap.isEmpty()) {
			return null;
		}

		// PdfReader reads from the input byte array stream.
		ByteArrayInputStream inputStream = new ByteArrayInputStream(inputPdfBytes);

		// PdfWriter writes to this output byte array stream.
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PdfReader reader = null;
		PdfWriter writer = null;
		PdfDocument pdfDocument = null;
		try {
			reader = new PdfReader(inputStream);
			writer = new PdfWriter(outputStream);
			pdfDocument = new PdfDocument(reader, writer);
			Set<Map.Entry<Integer, List<TokenModel>>> entrySet = pageTokenModelMap.entrySet();
			DeviceRgb maskColor = new DeviceRgb(204, 204, 204);
			float padding = 1.0f;
			for (Map.Entry<Integer, List<TokenModel>> entry : entrySet) {
				int pageNumber = entry.getKey();
				List<TokenModel> tokensTomask = entry.getValue();
				// get the canvas for drawing content over the existing content
				try {

					PdfPage page = pdfDocument.getPage(pageNumber);

					float actualPageHeight = page.getMediaBox().getHeight();
					float actualPageWidth = page.getMediaBox().getWidth();

					System.out.println("itext PageNumber:" + pageNumber + " height:" + actualPageHeight);
					System.out.println("itext Pagenumber:" + pageNumber + " width:" + actualPageWidth);

					PdfCanvas canvas = new PdfCanvas(page);
					canvas.setFillColor(maskColor);

					int rotationtouse = getinferrenceRotation(actualPageHeight, actualPageWidth,
							tokensTomask.get(0).getPageHeight(), tokensTomask.get(0).getPageWidth(),
							page.getRotation());

					System.out.println("page Number:" + pageNumber + " Reported rotation:" + page.getRotation()
							+ " inferred rotation:" + rotationtouse);

					// Draw the mask for each token
					for (TokenModel token : tokensTomask) {
						// --- prepare Rectangle Coordinated (using your corrected PDF bottom-up Y)

						float docAIHeight = token.getPageHeight();
						float docAIWidth = token.getPageWidth();

						float xScaleFactor;
						float yScalingFactor;
						float scaledMinX;
						float scaleMaxX;
						float scaledDocAITopY;
						float scaledDocAIBottomY;
						float lly;
						float ury;
						float llx;
						float width;
						float height;

						if (rotationtouse == 90) {

							float rotatedX_from_MinY = docAIHeight - token.getMaxY();
							float rotatedX_from_MaxY = docAIHeight - token.getMinY();

							float rotatedMinY = token.getMinX();
							float rotatedMaxY = token.getMaxX();

							float finalRotatedMinX = Math.min(rotatedX_from_MinY, rotatedX_from_MaxY);
							float finalRotatedMaxX = Math.max(rotatedX_from_MinY, rotatedX_from_MaxY);

							xScaleFactor = actualPageWidth / docAIHeight;
							yScalingFactor = actualPageHeight / docAIWidth;

							scaledMinX = finalRotatedMinX * xScaleFactor;
							scaleMaxX = finalRotatedMaxX * xScaleFactor;

							scaledDocAITopY = rotatedMinY * yScalingFactor;
							scaledDocAIBottomY = rotatedMaxY * yScalingFactor;

						} else {

							// if rotationtouse is 0
							xScaleFactor = actualPageWidth / docAIWidth;
							yScalingFactor = actualPageHeight / docAIHeight;

							scaledMinX = token.getMinX() * xScaleFactor;
							scaleMaxX = token.getMaxX() * xScaleFactor;

							scaledDocAITopY = token.getMinY() * yScalingFactor;
							scaledDocAIBottomY = token.getMaxY() * yScalingFactor;

						}

						// new y-axis inversion (using iText's true height)
						lly = actualPageHeight - scaledDocAIBottomY;
						ury = actualPageHeight - scaledDocAITopY;
						llx = scaledMinX;
						width = scaleMaxX - llx;
						height = ury - lly;
						// apply padding
						llx = llx - (padding / 2);
						lly = lly - (padding / 2);
						width = width + padding;
						height = height + padding;

						Rectangle rect = new Rectangle(llx, lly, width, height);
						canvas.rectangle(rect).fill();
					}

					canvas.release();

				} catch (PdfException e) {
					// TODO: handle exception
					e.printStackTrace();
				}

			}

		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			if (null != pdfDocument) {
				pdfDocument.close();
			}
		}

		return outputStream.toByteArray();
	}

	public static int getinferrenceRotation(float pageHeight, float pageWidth, float docAIHeight, float docAIWidth,
			int actualRotation) {
		if (actualRotation != 0)
			return actualRotation;
		final float TOLERANCE = 0.05f;
		float mediaBoxAspect = pageWidth / pageHeight;
		float docAIAspect = docAIWidth / docAIHeight;
		float docAIAspectInverted = docAIHeight / docAIWidth;

		if (Math.abs(mediaBoxAspect - docAIAspect) < TOLERANCE) {
			return 0;
		}

		if (Math.abs(mediaBoxAspect - docAIAspectInverted) < TOLERANCE) {

			return 90;
		}
		return actualRotation;
	}

	public static void performMasking(String filePath) {

		try {
			String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
			String formatName = fileName.split("\\.")[1];
			if (!("pdf".equalsIgnoreCase(formatName) || "jpg".equalsIgnoreCase(formatName)
					|| "jpeg".equalsIgnoreCase(formatName) || "png".equalsIgnoreCase(formatName))) {
				LOGGER.info("Only pdf,jpg,jpeg and png format are supported");
				return;
			}
			String mimeType = null;
			if ("pdf".equalsIgnoreCase(formatName)) {
				mimeType = "application/pdf";
			} else {
				if ("jpg".equalsIgnoreCase(formatName)) {
					mimeType = "image/jpg";
				} else {
					if ("jpeg".equalsIgnoreCase(formatName)) {
						mimeType = "image/jpeg";
					} else {
						mimeType = "image/png";
					}
				}
			}

			Path path = Paths.get(filePath);
			byte[] filebytes = Files.readAllBytes(path);
			List<TokenModel> tokenList = getTokenModelOfFindings(filebytes, mimeType);
			if (null != tokenList && !tokenList.isEmpty()) {
				printTokens(tokenList);
				sortTokenModelsByPageNumbers(tokenList);
				Map<Integer, List<TokenModel>> pageTokenModelMap = getPageTokenModelMap(tokenList);

				byte[] outputbytes = null;
				if ("pdf".equalsIgnoreCase(formatName)) {
					outputbytes = maskPdfTokenks(filebytes, pageTokenModelMap);
				} else {
					outputbytes = maskImageTokens(filebytes, pageTokenModelMap, formatName);
				}
				if (null != outputbytes) {
					String outfilePath = "/Users/lalamanil/AIPersonalClerk/SensitiveDataMaskoutputs/" + fileName;
					FileOutputStream fout = new FileOutputStream(new File(outfilePath));
					fout.write(outputbytes);
					System.out.println("Masked file created at:" + outfilePath);
					fout.close();
				} else {
					System.out.println("maskedFileoutput bytes is null");
				}
			}

		} catch (InvalidPathException e) {
			// TODO: handle exception
			e.printStackTrace();
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}

}
