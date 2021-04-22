/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.weasis.dicom.explorer;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Base64;

import org.dcm4che3.imageio.plugins.dcm.DicomMetaData;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.explorer.ObservableEvent;
import org.weasis.core.api.explorer.model.DataExplorerModel;
import org.weasis.core.api.gui.util.GuiExecutor;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.core.api.media.data.MediaSeriesGroupNode;
import org.weasis.core.api.media.data.Series;
import org.weasis.core.api.media.data.SeriesThumbnail;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.media.data.Thumbnail;
import org.weasis.core.ui.docking.UIManager;
import org.weasis.core.ui.editor.SeriesViewerFactory;
import org.weasis.core.ui.editor.ViewerPluginBuilder;
import org.weasis.dicom.codec.DicomMediaIO;
import org.weasis.dicom.codec.DicomSpecialElement;
import org.weasis.dicom.codec.TagD;
import org.weasis.dicom.codec.TagD.Level;
import org.weasis.core.api.explorer.DataExplorerView;
import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingModel;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingModel.CheckingMode;
import javax.swing.tree.TreePath;
import java.time.LocalDateTime; // import the LocalDateTime class
import org.weasis.dicom.codec.DicomSeries;

import org.weasis.dicom.explorer.LoadLocalDicom;

import java.io.File;
import java.util.Set;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.weasis.core.util.FileUtil;
import org.weasis.core.api.media.MimeInspector;
import org.weasis.dicom.codec.DicomCodec;
import org.weasis.core.ui.model.GraphicModel;
import org.weasis.core.ui.serialize.XmlSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Base64;

/**
 * @note This class is a pure copy of LoadLocalDicom taking care only of the
 *       DicomObject and not the file
 *
 * @version $Rev$ $Date$
 */

public class ImportDicomEndoData extends LoadLocalDicom {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LoadDicomObjects.class);

    private final DicomModel dicomModel;

    private String outputString = null;
    private String exportPath;

    public ImportDicomEndoData(File[] files, String outputPath, DicomModel dicomModel) {
        super(files, true, dicomModel); // $NON-NLS-1$

        this.dicomModel = dicomModel;
        this.exportPath = outputPath;
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        dicomModel.firePropertyChange(
                new ObservableEvent(ObservableEvent.BasicAction.LOADING_START, dicomModel, null, this));
        addSelectionAndnotify(files, true);
        return true;
    }

    @Override
    public void addSelectionAndnotify(File[] file, boolean firstLevel) {
        if (firstLevel) {
            LOGGER.info("Starting addSelectionAndnotify"); //$NON-NLS-1$
        }
        // Step 3 : collect info on dicom
        if (file == null || file.length < 1) {
            return;
        }
        final ArrayList<SeriesThumbnail> thumbs = new ArrayList<>();
        final ArrayList<File> folders = new ArrayList<>();

        for (int i = 0; i < file.length; i++) {
            if (isCancelled()) {
                return;
            }

            if (file[i] == null) {
                continue;
            } else if (file[i].isDirectory()) {
                folders.add(file[i]);
            } else {
                if (file[i].canRead()) {
                    if (FileUtil.isFileExtensionMatching(file[i], DicomCodec.FILE_EXTENSIONS)
                            || MimeInspector.isMatchingMimeTypeFromMagicNumber(file[i], DicomMediaIO.DICOM_MIMETYPE)) {
                        DicomMediaIO loader = new DicomMediaIO(file[i]);
                        if (loader.isReadableDicom()) {

                            // create `ObjectMapper` instance
                            ObjectMapper mapper = new ObjectMapper();
                            Attributes dicomObject = loader.getDicomObject();

                            // create a JSON object
                            ObjectNode output = mapper.createObjectNode();
                            output.put("ImportDateTime", LocalDateTime.now().toString());
                            output.put("ExportFolder", exportPath);
                            output.put("StudyDescription", dicomObject.getString(Tag.StudyDescription));
                            output.put("StudyID", dicomObject.getString(Tag.StudyID));
                            output.put("StudyDate", dicomObject.getString(Tag.StudyDate));
                            output.put("SeriesDate", dicomObject.getString(Tag.SeriesDate));
                            output.put("SeriesInstanceUID", dicomObject.getString(Tag.SeriesInstanceUID));
                            output.put("StudyInstanceUID", dicomObject.getString(Tag.StudyInstanceUID));

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
                                    cmd = "start ";
                                }
                                cmd += "endodata://weasis/dicom-import/" + encodedString;
                                Runtime run = Runtime.getRuntime();
                                Process pr = run.exec(cmd);
                                pr.waitFor();
                                BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                                String line = "";
                                while ((line = buf.readLine()) != null) {
                                    System.out.println(line);
                                }

                            } catch (Exception e) {
                                LOGGER.info("Dicom export error", e); //$NON-NLS-1$
                            }
                        }
                        // Issue: must handle adding image to viewer and building thumbnail (middle
                        // image)
                        SeriesThumbnail t = this.buildDicomStructure(loader, true);
                        if (t != null) {
                            thumbs.add(t);
                        }

                        File gpxFile = new File(file[i].getPath() + ".xml"); //$NON-NLS-1$
                        GraphicModel graphicModel = XmlSerializer.readPresentationModel(gpxFile);
                        if (graphicModel != null) {
                            loader.setTag(TagW.PresentationModel, graphicModel);
                        }
                    }
                }
            }
        }

        for (final SeriesThumbnail t : thumbs) {
            MediaSeries<MediaElement> series = t.getSeries();
            // Avoid to rebuild most of CR series thumbnail
            if (series != null && series.size(null) > 2) {
                GuiExecutor.instance().execute(t::reBuildThumbnail);
            }
        }
        for (int i = 0; i < folders.size(); i++) {
            addSelectionAndnotify(folders.get(i).listFiles(), false);
        }
    }

    @Override
    protected void done() {
        LOGGER.info("Starting export line 88"); //$NON-NLS-1$
        CheckTreeModel treeModel = new CheckTreeModel(dicomModel);
        TreeCheckingModel checkingModel = treeModel.getCheckingModel();
        checkingModel.setCheckingMode(CheckingMode.PROPAGATE_PRESERVING_UNCHECK);

        DataExplorerView explorer = UIManager.getExplorerplugin(DicomExplorer.NAME);
        Object studyInstanceUID = null;

        if (explorer instanceof DicomExplorer) {

            Set<Series> openSeriesSet = ((DicomExplorer) explorer).getSelectedPatientOpenSeries();
            Object rootNode = treeModel.getModel().getRoot();

            if (!openSeriesSet.isEmpty() && rootNode instanceof DefaultMutableTreeNode) {
                List<TreePath> selectedSeriesPathsList = new ArrayList<>();

                if (rootNode instanceof DefaultMutableTreeNode) {
                    Enumeration<?> enumTreeNode = ((DefaultMutableTreeNode) rootNode).breadthFirstEnumeration();
                    while (enumTreeNode.hasMoreElements()) {
                        Object child = enumTreeNode.nextElement();
                        if (child instanceof DefaultMutableTreeNode) {
                            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) child;
                            if (treeNode.getLevel() != 3) { // 3 stands for Series Level
                                continue;
                            }

                            Object userObject = treeNode.getUserObject();
                            if (userObject instanceof DicomSeries && openSeriesSet.contains(userObject)) {
                                studyInstanceUID = ((DicomSeries) userObject)
                                        .getTagValue(TagD.get(Tag.StudyInstanceUID));
                                selectedSeriesPathsList.add(new TreePath(treeNode.getPath()));
                            }
                        }
                    }
                }

                if (!selectedSeriesPathsList.isEmpty()) {
                    TreePath[] seriesCheckingPaths = selectedSeriesPathsList
                            .toArray(new TreePath[selectedSeriesPathsList.size()]);
                    checkingModel.setCheckingPaths(seriesCheckingPaths);
                    treeModel.setDefaultSelectionPaths(selectedSeriesPathsList);
                }
            }
        }
        if (studyInstanceUID != null) {

            File outputFolder = new File(
                    exportPath + File.separator + studyInstanceUID.toString().replaceAll("\\.", "_"));
            outputFolder.mkdir();
            LocalExport localExport = new LocalExport(dicomModel, treeModel, true);
            localExport.outputFolder = outputFolder;

            LOGGER.info("outputFolder=" + outputFolder.toString()); //$NON-NLS-1$
            LOGGER.info("studyInstanceUID=" + studyInstanceUID.toString()); //$NON-NLS-1$

            try {
                localExport.exportEndoDataDICOM(treeModel);

            } catch (Exception e) {
                LOGGER.info("Dicom export error", e); //$NON-NLS-1$
            }
        } else {
            LOGGER.info("NO DICOM FILE"); //$NON-NLS-1$
        }

        dicomModel.firePropertyChange(
                new ObservableEvent(ObservableEvent.BasicAction.LOADING_STOP, dicomModel, null, this));
        LOGGER.info("End of loading DICOM locally"); //$NON-NLS-1$
        return;
    }

}
