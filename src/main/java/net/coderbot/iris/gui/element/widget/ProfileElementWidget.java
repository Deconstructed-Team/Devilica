package net.coderbot.iris.gui.element.widget;

import net.coderbot.iris.Iris;
import net.coderbot.iris.gui.GuiUtil;
import net.coderbot.iris.gui.NavigationController;
import net.coderbot.iris.gui.screen.ShaderPackScreen;
import net.coderbot.iris.shaderpack.option.OptionSet;
import net.coderbot.iris.shaderpack.option.Profile;
import net.coderbot.iris.shaderpack.option.ProfileSet;
import net.coderbot.iris.shaderpack.option.menu.OptionMenuProfileElement;
import net.coderbot.iris.shaderpack.option.values.OptionValues;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import java.util.Optional;

public class ProfileElementWidget extends BaseOptionElementWidget<OptionMenuProfileElement> {

	private Profile next;
	private Profile previous;
	private String profileLabel;

	public ProfileElementWidget(OptionMenuProfileElement element) {
		super(element);
	}

	@Override
	public void init(ShaderPackScreen screen, NavigationController navigation) {
		super.init(screen, navigation);
		this.setLabel(I18n.format("options.iris.profile"));

		final ProfileSet profiles = this.element.profiles;
        final OptionSet options = this.element.options;
        final OptionValues pendingValues = this.element.getPendingOptionValues();

        final ProfileSet.ProfileResult result = profiles.scan(options, pendingValues);

		this.next = result.next;
		this.previous = result.previous;
        final Optional<String> profileName = result.current.map(p -> p.name);

		this.profileLabel = profileName.map(name -> GuiUtil.translateOrDefault(name, "profile." + name)).orElse(I18n.format("options.iris.profile.custom"));
	}

	@Override
	public void drawScreen(int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered) {
		this.updateRenderParams(width, width - (Minecraft.getMinecraft().fontRenderer.getStringWidth(I18n.format("options.iris.profile")) + 16));

		this.renderOptionWithValue(x, y, width, height, hovered);
	}

	@Override
	protected String createValueLabel() {
		return this.profileLabel;
	}

	@Override
	public Optional<String> getCommentTitle() {
		return Optional.of(I18n.format("options.iris.profile"));
	}

	@Override
	public String getCommentKey() {
		return "profile.comment";
	}

	@Override
	public boolean applyNextValue() {
		if (this.next == null) {
			return false;
		}

		Iris.queueShaderPackOptionsFromProfile(this.next);

		return true;
	}

	@Override
	public boolean applyPreviousValue() {
		if (this.previous == null) {
			return false;
		}

		Iris.queueShaderPackOptionsFromProfile(this.previous);

		return true;
	}

	@Override
	public boolean applyOriginalValue() {
		return false; // Resetting options is the way to return to the "default profile"
	}

	@Override
	public boolean isValueModified() {
		return false;
	}
}
