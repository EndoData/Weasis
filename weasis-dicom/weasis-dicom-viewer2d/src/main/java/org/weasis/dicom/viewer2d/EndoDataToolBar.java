/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.viewer2d;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import org.weasis.core.ui.editor.image.ImageViewerEventManager;
import org.weasis.core.ui.util.WtoolBar;
import org.weasis.dicom.codec.DicomImageElement;

import org.weasis.opencv.data.PlanarImage;
import org.weasis.core.api.image.SimpleOpManager;
import org.weasis.core.api.image.ZoomOp;
import org.opencv.imgcodecs.Imgcodecs;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.weasis.core.ui.editor.image.ViewCanvas;
import java.nio.file.Paths;

import org.weasis.core.api.media.data.MediaSeries;
import java.util.UUID;
import java.io.File;
import org.dcm4che3.data.Tag;
import org.weasis.dicom.codec.TagD;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Base64;

public class EndoDataToolBar extends WtoolBar {

    public EndoDataToolBar(final ImageViewerEventManager<DicomImageElement> eventManager, int index) {
        super(Messages.getString("EndoDataToolBar.screenshot"), index);
        if (eventManager == null) {
            throw new IllegalArgumentException("EventManager cannot be null");
        }
        final JButton screenButton = new JButton();
        screenButton.setToolTipText(Messages.getString("EndoDataToolBar.screenshot"));
        screenButton.setIcon(new ImageIcon(WtoolBar.class.getResource("/icon/32x32/camera.png"))); //$NON-NLS-1$
        screenButton.addActionListener(getScreenAction());
        add(screenButton);
    }

    public static ActionListener getScreenAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EventManager eventManager = EventManager.getInstance();

                ViewCanvas<org.weasis.dicom.codec.DicomImageElement> view2DPane = eventManager
                        .getSelectedView2dContainer().getSelectedImagePane();
                MediaSeries<org.weasis.dicom.codec.DicomImageElement> series = view2DPane.getSeries();
                // DicomModel dicomModel = (DicomModel) series.getTagValue(TagW.ExplorerModel);
                // MediaSeriesGroup studyGroup = dicomModel.getParent(series, DicomModel.study);

                Object dateObject = series.getTagValue(TagD.get(Tag.SeriesDate));
                if (dateObject == null) {
                    dateObject = series.getTagValue(TagD.get(Tag.StudyDate));
                }
                String date;
                if (dateObject == null) {
                    date = "0000-00-000";
                } else {
                    date = dateObject.toString();
                }

                String studyID = series.getTagValue(TagD.get(Tag.StudyID)) == null ? ""
                        : series.getTagValue(TagD.get(Tag.StudyID)).toString();

                String studyDescription = series.getTagValue(TagD.get(Tag.StudyDescription)) == null ? ""
                        : series.getTagValue(TagD.get(Tag.StudyDescription)).toString().toString();

                PlanarImage src = view2DPane.getSourceImage();
                if (src != null) {
                    SimpleOpManager opManager = view2DPane.getImageLayer().getDisplayOpManager().copy();
                    opManager.removeImageOperationAction(opManager.getNode(ZoomOp.OP_NAME));
                    opManager.setFirstNode(src);
                    String tempDir = System.getProperty("user.home", "") + File.separator + ".weasis" + File.separator //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            + "screenshots"; //$NON-NLS-1$
                    File destinationDir = new File(tempDir);

                    destinationDir.mkdirs();
                    String filePath = Paths.get(tempDir, "screenshot__" + UUID.randomUUID().toString() + ".jpg")
                            .toAbsolutePath().toString();
                    Imgcodecs.imwrite(filePath, opManager.process().toMat());

                    // create `ObjectMapper` instance
                    ObjectMapper mapper = new ObjectMapper();

                    // create a JSON object
                    ObjectNode output = mapper.createObjectNode();
                    output.put("filePath", filePath);
                    output.put("date", date);
                    output.put("studyID", studyID);
                    output.put("studyDescription", studyDescription);

                    try {
                        String json = mapper.writeValueAsString(output);
                        String encodedString = Base64.getUrlEncoder().encodeToString(json.getBytes());

                        boolean isMac;
                        String OS = System.getProperty("os.name", "generic").toLowerCase();
                        if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
                            isMac = true;
                        } else if (OS.indexOf("win") >= 0) {
                            isMac = false;
                        } else {
                            isMac = false; // Should throw actually.
                        }

                        String cmd;
                        if (isMac) {
                            cmd = "open ";
                        } else {
                            cmd = "cmd.exe /c start ";
                        }
                        cmd += "endodata://weasis/screenshot/" + encodedString;
                        Runtime run = Runtime.getRuntime();
                        Process pr = run.exec(cmd);
                        pr.waitFor();
                        BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                        String line = "";
                        while ((line = buf.readLine()) != null) {
                            System.out.println(line);
                        }
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                }
            }
        };
    }
}
