package net.launcher.profile;

import api.launcher.LaunchProfile;
import api.launcher.event.CollectSettingEvent;
import api.launcher.event.LaunchEvent;
import api.launcher.io.IOGuard;
import api.launcher.io.IOGuardContext;
import api.launcher.setting.*;
import javafx.collections.MapChangeListener;
import net.launcher.LaunchProfileImpl;
import net.launcher.game.nbt.NBT;
import net.launcher.game.nbt.NBTCompound;
import net.launcher.mod.SettingMod;
import net.wheatlauncher.SettingMinecraftImpl;
import org.to2mbn.jmccc.option.JavaEnvironment;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ci010
 */
public class IOGuardProfile extends IOGuard<LaunchProfileManager>
{
	private Path getProfilesRoot() {return getContext().getRoot().resolve("profiles");}

	private Path getProfileDir(String name) {return getProfilesRoot().resolve(name);}

	private Map<Class<? extends SettingType>, SettingType> settingTypeMap;
	private List<SettingType> settingTypes;

	@Override
	protected void onInit()
	{
		settingTypes = new ArrayList<>();
		settingTypeMap = new HashMap<>();
		CollectSettingEvent event = new CollectSettingEvent();
		event.register(SettingMinecraft.class, new SettingMinecraftImpl());
		event.register(SettingMods.class, new SettingMod());
		ARML.bus().postEvent(event);
		this.settingTypeMap = event.getLookup();
		this.settingTypes = event.getTypes();
	}

	private NBT serialize(LaunchProfile profile)
	{
		return NBT.compound().put("name", profile.getDisplayName()).put("id", profile.getId())
				.put("memory", profile.getMemory())
				.put("java", profile.getJavaEnvironment().getJavaPath().getAbsolutePath())
				.put("resolution", profile.getResolution().toString())
				.put("version", profile.getVersion());
	}

	private LaunchProfile deserialize(NBT nbt)
	{
		NBTCompound compound = nbt.asCompound();
		LaunchProfile launchProfile = new LaunchProfileImpl(compound.get("id").asString());
		launchProfile.setDisplayName(compound.get("name").asString());
		launchProfile.setJavaEnvironment(new JavaEnvironment(new File(compound.get("java").asString())));
		launchProfile.setMemory(compound.get("memory").asInt());
		launchProfile.setVersion(compound.get("version").asString());
		return launchProfile;
	}

	@Override
	public void forceSave() throws IOException
	{
		LaunchProfileManager instance = this.getInstance();
		if (instance == null) throw new IllegalStateException();
		new SaveManager().performance(null);
		for (LaunchProfile launchProfile : instance.getAllProfiles())
			new SaveProfile(launchProfile).performance(null);
	}

	@Override
	public LaunchProfileManager loadInstance() throws IOException
	{
		Files.createDirectories(getContext().getRoot().resolve("profiles"));
		Path path = this.getContext().getRoot().resolve("profiles.dat");
		List<String> profilesRecord = null;
		String selecting = null;

		if (Files.exists(path))
		{
			NBTCompound read = NBT.read(path, false).asCompound();
			selecting = read.get("selecting").asString();
			profilesRecord = read.get("profiles").asList().stream().map(NBT::asString).collect(Collectors.toList());
		}

		List<LaunchProfile> profilesList = new ArrayList<>();

		Files.walkFileTree(getProfilesRoot(), Collections.emptySet(), 2, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
			{
				Path prof = dir.resolve("profile.dat");
				if (!Files.exists(prof)) return super.preVisitDirectory(dir, attrs);
				LaunchProfile deserialize = deserialize(NBT.read(prof, false));
				profilesList.add(deserialize);
				for (SettingType settingType : settingTypes)
				{
					Setting load = settingType.load(dir);
					if (load != null) deserialize.addGameSetting(load);
				}
				return super.preVisitDirectory(dir, attrs);
			}
		});

		if (profilesRecord != null)
		{
			for (LaunchProfile profile : profilesList)
				profilesRecord.remove(profile.getId());
			if (!profilesRecord.isEmpty())
			{
				ARML.logger().warning("Bad profile record! " + profilesRecord);
			}
		}

		if (profilesList.isEmpty()) throw new IOException("profile.load.fail");

		LaunchProfileManager manager = new LaunchProfileManagerImpl(profilesList, settingTypes, settingTypeMap);
		if (selecting == null) selecting = profilesList.get(0).getId();
		manager.setSelectedProfile(selecting);

		return manager;
	}

	@Override
	public LaunchProfileManager defaultInstance()
	{
		LaunchProfileManager manager = new LaunchProfileManagerImpl(Collections.emptyList(),
				settingTypes, settingTypeMap);
		manager.setSelectedProfile(manager.newProfile("default").getId());
		return manager;
	}

	@Override
	protected void deploy() throws IOException
	{
		LaunchProfileManager instance = this.getInstance();
		SaveManager save = new SaveManager();
		this.getContext().registerSaveTask(save, instance.selectedProfileProperty(),
				instance.getAllProfiles());
		for (LaunchProfile profile : instance.getAllProfiles())
		{
			getContext().registerSaveTask(new SaveProfile(profile), profile.displayNameProperty(),
					profile.javaEnvironmentProperty(),
					profile.memoryProperty(),
					profile.versionProperty(),
					profile.resolutionProperty());
			for (Setting setting : profile.getAllGameSettings())
				setting.addListener(observable -> getContext().enqueue(new SaveSetting(profile.getId(), setting)));
			profile.gameSettingsProperty().addListener((MapChangeListener<String, Setting>) change ->
			{
				Setting valueAdded = change.getValueAdded();
				change.getValueAdded().addListener(observable -> getContext().enqueue(new SaveSetting(profile.getId(),
						valueAdded)));
			});
		}
		instance.getProfilesMap().addListener((MapChangeListener<String, LaunchProfile>) change ->
		{
			LaunchProfile profile = change.getValueAdded();
			if (profile != null)
			{
				getContext().registerSaveTask(new SaveProfile(profile), profile.displayNameProperty(),
						profile.javaEnvironmentProperty(),
						profile.memoryProperty(),
						profile.versionProperty(),
						profile.resolutionProperty());
			}
		});

		ARML.bus().addEventHandler(LaunchEvent.GAME_EXIT, event ->
		{
			isLoading = true;
			List<SettingType> list = ARML.core().getProfileSettingManager().getAllSettingType();
			try
			{
				Files.walkFileTree(getProfilesRoot(), Collections.emptySet(), 2, new SimpleFileVisitor<Path>()
				{
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
					{
						Path prof = dir.resolve("profile.dat");
						if (!Files.exists(prof)) return super.preVisitDirectory(dir, attrs);
						for (SettingType settingType : list)
						{
							Setting load = settingType.load(dir);
							if (load == null) continue;
							Optional<Setting> optional = event.getProfile().getGameSetting(settingType);
							if (optional.isPresent())
								for (SettingType.Option<?> option : settingType.getAllOption())
									loadSetting(option, optional.get(), load);
						}
						return super.preVisitDirectory(dir, attrs);
					}
				});
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			isLoading = false;
		});
	}

	private <T> void loadSetting(SettingType.Option<T> option, Setting setting, Setting load)
	{
		SettingProperty<T> a = setting.getOption(option);
		SettingProperty<T> b = load.getOption(option);
		a.setValue(b.getValue());
	}

	private boolean isLoading = false;

	class SaveSetting implements IOGuardContext.IOTask
	{
		String id;
		WeakReference<Setting> setting;

		SaveSetting(String id, Setting setting)
		{
			this.id = id;
			this.setting = new WeakReference<>(setting);
		}

		@Override
		public void performance(Path root) throws Exception
		{
			if (isLoading) return;
			Setting setting = this.setting.get();
			if (setting == null)
				return;
			if (!getInstance().getProfilesMap().containsKey(id))
				return;
			Path profileDir = getProfileDir(id);
			setting.getGameSettingType().save(profileDir, setting);
		}

		@Override
		public boolean isEquivalence(IOGuardContext.IOTask task)
		{
			if (task == this) return true;
			if (!(task instanceof SaveSetting)) return false;
			String id = ((SaveSetting) task).id;
			WeakReference<Setting> setting = ((SaveSetting) task).setting;
			return Objects.equals(id, this.id) && Objects.equals(setting.get(), this.setting.get());
		}
	}

	class SaveProfile implements IOGuardContext.IOTask
	{
		private String id;

		SaveProfile(LaunchProfile profile) {this.id = profile.getId();}

		@Override
		public void performance(Path root) throws IOException
		{
			if (!getInstance().getProfilesMap().containsKey(id))
				return;
			LaunchProfile launchProfile = getInstance().getProfilesMap().get(id);
			Path profileDir = getProfileDir(launchProfile.getId());
			Path resolve = profileDir.resolve("profile.dat");
			NBT.write(resolve, serialize(launchProfile).asCompound(), false);
		}

		@Override
		public boolean isEquivalence(IOGuardContext.IOTask task)
		{
			return task == this ||
					(task instanceof SaveProfile && Objects.equals(((SaveProfile) task).id, id));
		}
	}

	class SaveManager implements IOGuardContext.IOTask
	{
		@Override
		public void performance(Path root) throws IOException
		{
			Path path = getContext().getRoot().resolve("profiles.dat");
			LaunchProfileManager instance = getInstance();
			if (instance == null) throw new IllegalStateException();

			NBTCompound compound = NBT.compound();
			compound.put("selecting", instance.getSelectedProfile());
			compound.put("profiles", NBT.listStr(instance.getAllProfiles().stream().map(LaunchProfile::getId).collect(Collectors.toList())));
			NBT.write(path, compound, false);
		}

		@Override
		public boolean isEquivalence(IOGuardContext.IOTask task)
		{
			return task == this || task instanceof SaveManager;
		}
	}
}
