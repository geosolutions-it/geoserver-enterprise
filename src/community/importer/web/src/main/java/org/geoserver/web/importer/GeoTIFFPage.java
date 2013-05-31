/**
 * Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * $Id: GeoTIFFPage.java 174 2012-01-23 15:11:17Z alessio $
 */
package org.geoserver.web.importer;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriter;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.workspace.WorkspaceChoiceRenderer;
import org.geoserver.web.data.workspace.WorkspaceDetachableModel;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SRSListPanel;
import org.geoserver.web.wicket.browser.ExtensionFileFilter;
import org.geoserver.web.wicket.browser.GeoServerFileChooser;
import org.geotools.utils.CoverageToolsConstants;
import org.geotools.utils.imageoverviews.OverviewsEmbedder;


/**
 * Sets up the import process for GeoTIFFs and starts it up delegating the progress to
 * {@link ImportProgressPage}
 *
 * TODO: single out common behaviour with DirectoryPage and make a common superclass of both
 *
 * @author Luca Morandini lmorandini@ieee.org
 * @author Andrea Aime, GeoSolutions SAS
 *
 */
@SuppressWarnings("serial")
public class GeoTIFFPage extends ImporterSecuredPage
{
    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(".tiff", ".tif");

    public static final IOFileFilter INTERNAL_FILTER = FileFilterUtils.makeCVSAware(
            FileFilterUtils.makeSVNAware(
                FileFilterUtils.makeFileOnly(
                    new WildcardFileFilter(
                        new ArrayList<String>()
                        {

                            {
                                add("*.tif");
                                add("*.tiff");
                            }
                        }, IOCase.INSENSITIVE))));

    // Model properties
    private WorkspaceDetachableModel workspaceModel;

    private String directory = "";

    private String outdirectory = "";

    private String defaultsrs = "EPSG:4326";

    private boolean copy = false;

    private boolean overview = false;

    private boolean tile = false;

    private String compressiontype = "NONE";

    private int compressionratio = (new Double(CoverageToolsConstants.DEFAULT_COMPRESSION_RATIO * 100)).intValue();

    private int tilewidth = CoverageToolsConstants.DEFAULT_INTERNAL_TILE_WIDTH;

    private int tileheight = CoverageToolsConstants.DEFAULT_INTERNAL_TILE_HEIGHT;

    private boolean rettile = true;

    private int noverview = 2;

    private int downsamplestep = 2;

    private String subsamplealgorithm = (OverviewsEmbedder.SubsampleAlgorithm.values()[0]).toString();

    private boolean extoverview = false;

    private boolean retoverview = true;

    // Widgets properties
    private GeoServerDialog dialog;

    private TextField<String> dirField;

    private TextField<String> outdirField;

    private Form form;

    private DropDownChoice<String> wsChoice;

    private AjaxCheckBox copyField;

    private DropDownChoice<String> compressiontypeField;

    private TextField<Integer> compressionratioField;

    private TileCheckBox tileField;

    private TextField<Integer> tilewidthField;

    private TextField<Integer> tileheightField;

    private CheckBox rettileField;

    private OverviewCheckBox overviewField;

    private DropDownChoice<Integer> noverviewField;

    private DropDownChoice<Integer> downsamplestepField;

    private DropDownChoice<String> subsamplealgorithmField;

    private CheckBox extoverviewField;

    private CheckBox retoverviewField;

    private TextField<String> defaultsrsField;

    /*
     * Constructor
     */
    public GeoTIFFPage()
    {

        // Adds form to the popup dialog
        this.form = new Form("form", new CompoundPropertyModel(this));
        this.add(this.form);
        this.dialog = new GeoServerDialog("dialog");
        this.form.add(this.dialog);

        // Image creation widgets creation and setting

        // When the copy field is checked, enables output dir field
        this.copyField = new AjaxCheckBox("copy")
            {
                @Override
                protected void onUpdate(AjaxRequestTarget target)
                {
                    boolean flag = Boolean.valueOf(copyField.getValue());
                    outdirectory = buildOutputDirectory(".");
                    outdirField.setEnabled(flag);
                    form.get("outchooser").setEnabled(flag);
                    target.addComponent(outdirField);
                    target.addComponent(form.get("outchooser"));
                }
            };
        this.copyField.setRequired(false);
        this.copyField.setOutputMarkupId(true);
        this.form.add(this.copyField);

        // Choice of compression type
        List<String> compressiontypes = new ArrayList<String>();
        compressiontypes.add("NONE");
        compressiontypes.addAll(Arrays.asList(TIFFImageWriter.compressionTypes));
        this.compressiontypeField = new DropDownChoice<String>("compressiontype",
                new PropertyModel<String>(this, "compressiontype"), compressiontypes);
        this.compressiontypeField.add(new AjaxFormComponentUpdatingBehavior("onchange")
            {
                protected void onUpdate(AjaxRequestTarget target)
                {
                    compressionratioField.setEnabled(!((compressiontype == null) || "NONE".equals(compressiontype)));
                    target.addComponent(compressionratioField);
                }
            });
        this.compressiontypeField.setRequired(false);
        this.compressiontypeField.setOutputMarkupId(true);
        this.form.add(this.compressiontypeField);

        // Compression ratio
        this.compressionratioField = new TextField<Integer>("compressionratio");
        this.compressionratioField.setRequired(true);
        this.compressionratioField.setEnabled(false);
        this.compressionratioField.setOutputMarkupId(true);
        this.compressionratioField.add(new RangeValidator<Integer>(0, 100));
        this.form.add(this.compressionratioField);

        // Field to specify the use of tiles
        this.tileField = new TileCheckBox("tile");
        this.tileField.setRequired(false);
        this.form.add(this.tileField);

        // Fields to specify tile size
        this.tilewidthField = new TextField<Integer>("tilewidth");
        this.tilewidthField.setRequired(true);
        this.tilewidthField.setEnabled(false);
        this.tilewidthField.setOutputMarkupId(true);
        this.tilewidthField.add(new RangeValidator(64, 2048)); // SG make this parametric somehow,
        // anyway, less than 64 is useless
        this.form.add(this.tilewidthField);
        this.tileheightField = new TextField<Integer>("tileheight");
        this.tileheightField.setRequired(true);
        this.tileheightField.setEnabled(false);
        this.tileheightField.setOutputMarkupId(true);
        this.tileheightField.add(new RangeValidator(64, 2048));
        this.form.add(this.tileheightField);

        // Field to specify the re-use of tiles, if existing
        this.rettileField = new CheckBox("rettile");
        this.rettileField.setRequired(false);
        this.rettileField.setEnabled(false);
        this.rettileField.setOutputMarkupId(true);
        this.form.add(this.rettileField);

        // Field to specify the use of overview
        this.overviewField = new OverviewCheckBox("overview");
        this.overviewField.setRequired(false);
        this.overviewField.setOutputMarkupId(true);
        this.form.add(this.overviewField);

        // Choice of downsample steps
        List<Integer> downsamplesteps = Arrays.asList(new Integer[] { 2, 3, 4, 6, 7, 8 });
        this.downsamplestepField = new DropDownChoice<Integer>("downsamplestep",
                new PropertyModel<Integer>(this, "downsamplestep"), downsamplesteps);
        this.downsamplestepField.setRequired(false);
        this.downsamplestepField.setEnabled(false);
        this.downsamplestepField.setOutputMarkupId(true);
        this.form.add(this.downsamplestepField);

        // Choice of downsample algorithm
        List<String> subsamplealgorithms = new ArrayList<String>();
        for (OverviewsEmbedder.SubsampleAlgorithm alg : OverviewsEmbedder.SubsampleAlgorithm.values())
        {
            subsamplealgorithms.add(alg.toString());
        }
        this.subsamplealgorithmField = new DropDownChoice<String>("subsamplealgorithm",
                new PropertyModel<String>(this, "subsamplealgorithm"), subsamplealgorithms);
        this.subsamplealgorithmField.setRequired(false);
        this.subsamplealgorithmField.setEnabled(false);
        this.subsamplealgorithmField.setOutputMarkupId(true);
        this.form.add(this.subsamplealgorithmField);

        // Choice of n. of overviews
        List<Integer> noverviews = Arrays.asList(
                new Integer[]
                {
                    1, 2, 3, 4, 6, 7, 8, 9, 10, 11,
                    12, 13, 14, 15, 16, 17, 18, 18, 20, 21, 22, 23, 24
                });
        this.noverviewField = new DropDownChoice<Integer>("noverview", new PropertyModel<Integer>(
                    this, "noverview"), noverviews);
        this.noverviewField.setRequired(false);
        this.noverviewField.setEnabled(false);
        this.noverviewField.setOutputMarkupId(true);
        this.form.add(this.noverviewField);

        // Field to specify the re-use of overview, if existing
        this.retoverviewField = new CheckBox("retoverview");
        this.retoverviewField.setRequired(false);
        this.retoverviewField.setEnabled(false);
        this.retoverviewField.setOutputMarkupId(true);
        this.form.add(this.retoverviewField);

        // Field to specify the use of external overview
        this.extoverviewField = new CheckBox("extoverview");
        this.extoverviewField.setRequired(false);
        this.extoverviewField.setEnabled(false);
        this.extoverviewField.setOutputMarkupId(true);
        this.form.add(this.extoverviewField);

        // Workspace chooser

        final boolean thereAreWorkspaces = !getCatalog().getWorkspaces().isEmpty();

        if (!thereAreWorkspaces)
        {
            final String message = (String) new ResourceModel("NewDataPage.noWorkspacesErrorMessage").getObject();
            super.error(message);
            throw new IllegalStateException(message);
        }

        this.workspaceModel = new WorkspaceDetachableModel(getCatalog().getDefaultWorkspace());
        this.wsChoice = new DropDownChoice<String>("workspace", workspaceModel,
                new WorkspacesModel(), new WorkspaceChoiceRenderer());
        this.wsChoice.setOutputMarkupId(true);
        this.form.add(this.wsChoice);

        // Adds workspace link
        this.form.add(createWorkspaceLink());

        // Field to specify directory
        this.dirField = new TextField<String>("directory");
        this.dirField.add(new DirectoryValidator()
            {
                @Override
                protected void onValidate(IValidatable validatable)
                {

                    if ((copy == false) && ((tile == true) || (overview == true)))
                    {
                        try
                        {
                            String directory = (String) validatable.getValue();
                            File file = new File(directory);
                            if (!file.isDirectory())
                            {
                                file = file.getParentFile();
                            }
                            if (!file.canRead() || !file.canWrite())
                            {
                                error(validatable, "ImporterSecuredPage.notEnoughPermission");
                            }
                        }
                        catch (Exception e)
                        {
                            if (LOGGER.isLoggable(Level.WARNING))
                            {
                                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
                            }
                            error(validatable, "ImporterSecuredPage.noData");
                        }
                    }

                    super.onValidate(validatable);
                }
            });
        this.dirField.setRequired(true);
        this.dirField.setOutputMarkupId(true);
        this.form.add(this.dirField);
        this.form.add(chooserButton(this.form));

        // Field to specify output directory
        this.outdirField = new TextField<String>("outdirectory");
        this.outdirField.add(new OutDirectoryValidator());
        this.outdirField.setRequired(false);
        this.outdirField.setEnabled(false);
        this.outdirField.setOutputMarkupId(true);
        this.form.add(this.outdirField);
        this.form.add(outChooserButton(this.form));

        // Adds link to choose default SRS
        this.form.add(srsChooserLink(form));

        // Field containing default SRS
        this.defaultsrsField = new TextField<String>("defaultsrs");
        this.defaultsrsField.setRequired(true);
        this.defaultsrsField.setOutputMarkupId(true);
        this.form.add(this.defaultsrsField);

        // Submit button
        SubmitLink submitLink = submitLink();
        this.form.add(submitLink);
        this.form.setDefaultButton(submitLink);
    }

    /*
     * Widget for creating the workspace
     */
    private AjaxLink createWorkspaceLink()
    {
        return new AjaxLink("createWorkspace")
            {

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    dialog.setTitle(new ParamResourceModel("dialogTitle", GeoTIFFPage.this));
                    dialog.setInitialWidth(400);
                    dialog.setInitialHeight(150);
                    dialog.setMinimalHeight(150);

                    dialog.showOkCancel(target, new DialogDelegate()
                        {
                            String wsName;

                            @Override
                            protected boolean onSubmit(AjaxRequestTarget target, Component contents)
                            {
                                try
                                {
                                    NewWorkspacePanel panel = (NewWorkspacePanel) contents;
                                    wsName = panel.workspace;

                                    final Catalog catalog = getCatalog();
                                    final CatalogFactory factory = catalog.getFactory();
                                    WorkspaceInfo ws = factory.createWorkspace();
                                    ws.setName(wsName);

                                    NamespaceInfo ns = factory.createNamespace();
                                    ns.setPrefix(wsName);
                                    // FIXME: change it ?
                                    ns.setURI("http://opengeo.org/#" + URLEncoder.encode(wsName, "ASCII"));

                                    catalog.add(ws);
                                    catalog.add(ns);

                                    return true;
                                }
                                catch (Exception e)
                                {
                                    if (LOGGER.isLoggable(Level.FINE))
                                    {
                                        LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
                                    }

                                    return false;
                                }
                            }

                            @Override
                            public void onClose(AjaxRequestTarget target)
                            {
                                workspaceModel = new WorkspaceDetachableModel(getCatalog().getWorkspaceByName(wsName));
                                wsChoice.setModel(workspaceModel);
                                target.addComponent(wsChoice);
                            }

                            @Override
                            protected Component getContents(String id)
                            {
                                return new NewWorkspacePanel(id);
                            }
                        });
                }
            };
    }

    /*
     * Widget for choosing the input directory
     */
    private Component chooserButton(Form form)
    {
        AjaxSubmitLink link = new AjaxSubmitLink("chooser")
            {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form)
                {
                    dialog.setTitle(new ParamResourceModel("chooseDirectory", this));
                    dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate()
                        {

                            @Override
                            protected Component getContents(String id)
                            {
                                // use what the user currently typed
                                File file = null;
                                if (!dirField.getInput().trim().equals(""))
                                {
                                    file = new File(dirField.getInput());
                                    if (!file.exists())
                                    {
                                        file = null;
                                    }
                                }

                                GeoServerFileChooser chooser = new GeoServerFileChooser(id,
                                        new Model(file))
                                    {
                                        // In case a single file is selected, sets it as importer input and closes
                                        // the dialog
                                        protected void fileClicked(File file, AjaxRequestTarget target)
                                        {
                                            this.setDefaultModel(new Model(file));
                                            directory = file.getAbsolutePath();

                                            // clear the raw input of the field won't show the new model value
                                            dirField.clearInput();
                                            target.addComponent(dirField);
                                            dialog.close(target);
                                        }
                                    };
                                chooser.setFilter(new Model(GeoTIFFPage.FILE_FILTER));

                                return chooser;
                            }

                            @Override
                            protected boolean onSubmit(AjaxRequestTarget target, Component contents)
                            {
                                GeoServerFileChooser chooser = (GeoServerFileChooser) contents;
                                directory = ((File) chooser.getDefaultModelObject()).getAbsolutePath();
                                // clear the raw input of the field won't show the new model value
                                dirField.clearInput();

                                target.addComponent(dirField);

                                return true;
                            }

                            @Override
                            public void onClose(AjaxRequestTarget target)
                            {
                                // update the field with the user chosen value
                                target.addComponent(dirField);
                            }

                        });

                }

            };
        // otherwise the link won't trigger when the form contents are not valid
        link.setDefaultFormProcessing(false);

        return link;
    }

    /*
     * Widget for choosing the output directory
     */
    private Component outChooserButton(Form form)
    {
        AjaxSubmitLink link = new AjaxSubmitLink("outchooser")
            {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form)
                {
                    dialog.setTitle(new ParamResourceModel("chooseDirectory", this));
                    dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate()
                        {

                            @Override
                            protected Component getContents(String id)
                            {
                                // use what the user currently typed
                                File file = null;
                                if (!outdirField.getInput().trim().equals(""))
                                {
                                    file = new File(outdirField.getInput());
                                    if (!file.exists())
                                    {
                                        file = null;
                                    }
                                }

                                GeoServerFileChooser outChooser = new GeoServerFileChooser(id,
                                        new Model(file));

                                return outChooser;
                            }

                            @Override
                            protected boolean onSubmit(AjaxRequestTarget target, Component contents)
                            {
                                GeoServerFileChooser outChooser = (GeoServerFileChooser) contents;
                                outdirectory = ((File) outChooser.getDefaultModelObject()).getAbsolutePath();

                                // clear the raw input of the field won't show the new model value
                                outdirField.clearInput();

                                target.addComponent(outdirField);

                                return true;
                            }

                            @Override
                            public void onClose(AjaxRequestTarget target)
                            {
                                // update the field with the user chosen value
                                target.addComponent(outdirField);
                            }

                        });

                }

            };
        // otherwise the link won't trigger when the form contents are not valid
        link.setDefaultFormProcessing(false);
        link.setEnabled(false); // This button can be clicked only when imported data are copied

        return link;
    }

    /*
     * Widget for choosing the default SRS
     */
    private Component srsChooserLink(Form form)
    {
        AjaxSubmitLink link = new AjaxSubmitLink("srschooser")
            {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form)
                {

                    dialog.setTitle(new ParamResourceModel("chooseSrs", this));

                    dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate()
                        {

                            private String chosensrs = defaultsrs;

                            public void onClose(AjaxRequestTarget target)
                            {
                                target.addComponent(defaultsrsField);
                            }

                            @Override
                            protected Component getContents(String id)
                            {
                                return (new SRSListPanel("userPanel")
                                        {
                                            protected void onCodeClicked(AjaxRequestTarget target, String epsgCode)
                                            {
                                                chosensrs = "EPSG:" + epsgCode;
                                            }
                                        });
                            }

                            protected boolean onCancel(AjaxRequestTarget target, Component contents)
                            {
                                return true;
                            }

                            @Override
                            protected boolean onSubmit(AjaxRequestTarget target, Component contents)
                            {
                                defaultsrs = chosensrs;

                                return true;
                            }

                        });

                }
            };

        // otherwise the link won't trigger when the form contents are not valid
        link.setDefaultFormProcessing(false);

        return link;
    }

    /*
     * Widget for going to the next step of the import process
     */
    SubmitLink submitLink()
    {
        return new SubmitLink("next")
            {

                @Override
                public void onSubmit()
                {
                    try
                    {

                        // Gets nomespace and workspace
                        WorkspaceInfo workspace = (WorkspaceInfo) workspaceModel.getObject();
                        NamespaceInfo namespace = getCatalog().getNamespaceByPrefix(workspace.getName());

                        // Redirects to the layer chooser
                        PageParameters pp = new PageParameters();
                        pp.put("workspace", workspace.getName());
                        pp.put("directory", directory);

                        // If data are not to be copied, output directory should be equal to the input directory
                        if (copy == false)
                        {
                            File inputDirectory = new File(directory);
                            pp.put("outdirectory", (!inputDirectory.isDirectory()) ? inputDirectory.getParent() : inputDirectory.getAbsolutePath());

                        }
                        else
                        {
                            pp.put("outdirectory", outdirectory);
                        }

                        pp.put("workspaceNew", false);
                        pp.put("copy", copy);
                        pp.put("tile", tile);
                        pp.put("overview", overview);
                        pp.put("compressiontype", compressiontype);
                        pp.put("compressionratio", compressionratio);
                        pp.put("tilewidth", tilewidth);
                        pp.put("tileheight", tileheight);
                        pp.put("rettile", rettile);
                        pp.put("downsamplestep", downsamplestep);
                        pp.put("subsamplealgorithm", subsamplealgorithm);
                        pp.put("noverview", noverview);
                        pp.put("retoverview", retoverview);
                        pp.put("extoverview", extoverview);
                        pp.put("defaultsrs", defaultsrs);

                        this.setResponsePage(RasterChooserPage.class, pp);
                    }
                    catch (Exception e)
                    {
                        LOGGER.log(Level.SEVERE, "Error while setting up mass import", e);
                    }

                }
            };
    }

    /**
     * Check box holding the tiling options
     */
    private final class TileCheckBox extends AjaxCheckBox
    {
        TileCheckBox(String id)
        {
            super(id);
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target)
        {
            boolean flag = Boolean.valueOf(tileField.getValue());
            this.enableDependingWidgets(flag, target);
        }

        public void enableDependingWidgets(boolean flag, AjaxRequestTarget target)
        {
            tilewidthField.setEnabled(flag);
            tileheightField.setEnabled(flag);
            rettileField.setEnabled(flag);
            target.addComponent(tilewidthField);
            target.addComponent(tileheightField);
            target.addComponent(rettileField);
        }
    }

    /**
     * Check box holding the overview options
     */
    private final class OverviewCheckBox extends AjaxCheckBox
    {
        OverviewCheckBox(String id)
        {
            super(id);
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target)
        {
            boolean flag = Boolean.valueOf(overviewField.getValue());
            this.enableDependingWidgets(flag, target);
        }

        public void enableDependingWidgets(boolean flag, AjaxRequestTarget target)
        {
            downsamplestepField.setEnabled(flag);
            noverviewField.setEnabled(flag);
            subsamplealgorithmField.setEnabled(flag);
            extoverviewField.setEnabled(flag);
            retoverviewField.setEnabled(flag);
            target.addComponent(downsamplestepField);
            target.addComponent(noverviewField);
            target.addComponent(subsamplealgorithmField);
            target.addComponent(extoverviewField);
            target.addComponent(retoverviewField);
            subsamplealgorithmField.modelChanged();
            downsamplestepField.modelChanged();
            noverviewField.modelChanged();
            extoverviewField.modelChanged();
            retoverviewField.modelChanged();
        }
    }

}
