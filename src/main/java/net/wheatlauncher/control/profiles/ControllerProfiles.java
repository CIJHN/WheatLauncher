package net.wheatlauncher.control.profiles;

import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXTabPane;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;
import net.launcher.Logger;
import net.launcher.assets.MinecraftVersion;
import net.launcher.control.ComboBoxDelegate;
import net.launcher.profile.LaunchProfile;
import net.wheatlauncher.MainApplication;
import net.wheatlauncher.control.mics.ControllerMinecraftVersionChooserPane;
import net.wheatlauncher.control.mics.ControllerProfileChooserPane;

import javax.annotation.PreDestroy;


/**
 * @author ci010
 */
public class ControllerProfiles
{
	public ComboBoxDelegate<LaunchProfile> profile;

	public ComboBoxDelegate<MinecraftVersion> versions;

	public JFXTabPane optionsTab;

	public StackPane gameSetting;

	public StackPane languageSetting;

	public StackPane resourcePackSetting;

	public StackPane modSetting;

	public JFXDialog rootDialog;
	public StackPane versionChooser;
	public ControllerMinecraftVersionChooserPane versionChooserController;
	public StackPane profileChooser;
	public ControllerProfileChooserPane profileChooserController;


	public void initialize()
	{
		Logger.trace("init");
		rootDialog.setOverlayClose(true);
		initVersion();
		initProfile();
		versionChooserController.initialize(versions, () -> (StackPane) rootDialog.getScene().getRoot());
		profileChooserController.initialize(profile);
	}

	@PreDestroy
	public void distroy()
	{
		Logger.trace("destroy");
	}

	private void initVersion()
	{
		versions.setStringConverter(new StringConverter<MinecraftVersion>()
		{
			@Override
			public String toString(MinecraftVersion object)
			{
				if (object != null)
					return object.toString();
				return "";
			}

			@Override
			public MinecraftVersion fromString(String string)
			{
				return null;
			}
		});
		profile.setStringConverter(new StringConverter<LaunchProfile>()
		{
			@Override
			public String toString(LaunchProfile object)
			{
				if (object != null)
					return object.getDisplayName();
				return "";
			}

			@Override
			public LaunchProfile fromString(String string)
			{
				return null;
			}
		});
		MainApplication.getCore().getProfileManager().selectedProfileProperty().addListener(observable ->
		{
			String version = MainApplication.getCore().getProfileManager().selecting().getVersion();
			if (version != null) versions.setValue(MainApplication.getCore().getAssetsManager().getVersion(version));
		});
		LaunchProfile selecting = MainApplication.getCore().getProfileManager().selecting();
		if (selecting != null)
		{
			String version = selecting.getVersion();
			if (version != null)
				versions.setValue(MainApplication.getCore().getAssetsManager().getVersion(version));
		}
		versions.valueProperty().addListener(observable ->
		{
			MinecraftVersion value = versions.getValue();
			if (value != null)
				MainApplication.getCore().getProfileManager().selecting().setVersion(value.getVersionID());
		});
	}

	private void initProfile()
	{
//		profile.setProfiles(MainApplication.getCore().getProfileManager().getAllProfiles());
//		profile.setProfileFactory(param -> MainApplication.getCore().getProfileManager().newProfile(param));
//		profile.setRemoveCallback(param ->
//		{
//			MainApplication.getCore().getProfileManager().deleteProfile(param.getId());
//			return null;
//		});
		LaunchProfile selecting = MainApplication.getCore().getProfileManager().selecting();
		if (selecting != null)
			profile.setValue(selecting);
		profile.valueProperty().addListener(observable ->
				MainApplication.getCore().getProfileManager().setSelectedProfile(profile.getValue().getId()));
	}
}
