package ironartichoke.sketchpad.util;

import android.content.Context;

import de.psdev.licensesdialog.LicenseResolver;
import de.psdev.licensesdialog.licenses.License;
import ironartichoke.sketchpad.R;

/**
 * A utility class for preparing licenses for display in the Notices dialog.
 */
public final class LicenseUtils
{
	/**
	 * Registers some extra licenses for display in the Notices dialog.
	 * @param ctx Any context.
	 */
	public static void registerExtraLicenses(final Context ctx)
	{
		// Add the CC Attribution 4.0 International license for icons by Google.
		LicenseResolver.registerLicense(new License()
		{
			@Override
			public String getName()
			{
				return ctx.getString(R.string.license_cc_by_4_name);
			}

			@Override
			public String readSummaryTextFromResources(Context context)
			{
				return ctx.getString(R.string.license_cc_by_4_text);
			}

			@Override
			public String readFullTextFromResources(Context context)
			{
				return ctx.getString(R.string.license_cc_by_4_text);
			}

			@Override
			public String getVersion()
			{
				return "4.0";
			}

			@Override
			public String getUrl()
			{
				return ctx.getString(R.string.license_cc_by_4_url);
			}
		});

		// Add the CC Attribution 3.0 license for Riven's atan2 code.
		LicenseResolver.registerLicense(new License()
		{
			@Override
			public String getName()
			{
				return ctx.getString(R.string.license_cc_by_3_name);
			}

			@Override
			public String readSummaryTextFromResources(Context context)
			{
				return ctx.getString(R.string.license_cc_by_3_text);
			}

			@Override
			public String readFullTextFromResources(Context context)
			{
				return ctx.getString(R.string.license_cc_by_3_text);
			}

			@Override
			public String getVersion()
			{
				return "3.0";
			}

			@Override
			public String getUrl()
			{
				return ctx.getString(R.string.license_cc_by_3_url);
			}
		});

		// Add the SIL Open Font License for icons by the Community.
		LicenseResolver.registerLicense(new License()
		{
			@Override
			public String getName()
			{
				return ctx.getString(R.string.license_ofl_name);
			}

			@Override
			public String readSummaryTextFromResources(Context context)
			{
				return ctx.getString(R.string.license_ofl_text);
			}

			@Override
			public String readFullTextFromResources(Context context)
			{
				return ctx.getString(R.string.license_ofl_text);
			}

			@Override
			public String getVersion()
			{
				return "1.1";
			}

			@Override
			public String getUrl()
			{
				return ctx.getString(R.string.license_ofl_url);
			}
		});
	}
}
