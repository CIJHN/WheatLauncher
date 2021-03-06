package api.launcher.auth;


import net.launcher.utils.Patterns;
import net.launcher.utils.StringUtils;
import org.to2mbn.jmccc.auth.AuthInfo;
import org.to2mbn.jmccc.auth.AuthenticationException;
import org.to2mbn.jmccc.auth.Authenticator;
import org.to2mbn.jmccc.auth.yggdrasil.CharacterSelector;
import org.to2mbn.jmccc.auth.yggdrasil.YggdrasilAuthenticator;
import org.to2mbn.jmccc.auth.yggdrasil.core.ProfileService;
import org.to2mbn.jmccc.auth.yggdrasil.core.yggdrasil.YggdrasilProfileServiceBuilder;

import java.io.Serializable;


/**
 * @author ci010
 */
public class AuthorizeMojang extends YggdrasilAuthenticator implements Authorize, Serializable
{
	private transient String account;
	private transient String password;

	@Override
	public String getId()
	{
		return "mojang";
	}

	@Override
	public void setAccount(String account)
	{
		if (StringUtils.isEmpty(account))
			throw new NullPointerException("mojang.account.null");
		else if (!Patterns.EMAIL.matcher(account).matches())
			throw new IllegalArgumentException("mojang.account.invalid");
		this.account = account;
	}

	@Override
	public String getAccount()
	{
		return account;
	}

	@Override
	public void updatePassword(String password)
	{
		if (StringUtils.isEmpty(password))
			throw new NullPointerException("mojang.password.null");
		else if (password.length() < 6)
			throw new IllegalArgumentException("mojang.password.invalid");
		this.password = password;
	}

	@Override
	public ProfileService createProfileService()
	{
		return YggdrasilProfileServiceBuilder.buildDefault();
	}

	@Override
	protected PasswordProvider tryPasswordLogin() throws AuthenticationException
	{
		return new PasswordProvider()
		{
			@Override
			public String getUsername() throws AuthenticationException
			{
				return account;
			}

			@Override
			public String getPassword() throws AuthenticationException
			{
				return password;
			}

			@Override
			public CharacterSelector getCharacterSelector()
			{
				return null;
			}
		};
	}

	@Override
	public synchronized AuthInfo auth() throws AuthenticationException
	{
		return super.auth();
	}

	@Override
	public Authenticator buildAuthenticator()
	{
		return this;
	}
}
