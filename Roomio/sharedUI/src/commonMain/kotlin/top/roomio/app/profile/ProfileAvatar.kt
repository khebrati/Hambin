package top.roomio.app.profile

import org.jetbrains.compose.resources.DrawableResource
import roomio.sharedui.generated.resources.Res
import roomio.sharedui.generated.resources.avatar_berry
import roomio.sharedui.generated.resources.avatar_bloom
import roomio.sharedui.generated.resources.avatar_cloud
import roomio.sharedui.generated.resources.avatar_comet
import roomio.sharedui.generated.resources.avatar_echo
import roomio.sharedui.generated.resources.avatar_ember
import roomio.sharedui.generated.resources.avatar_mint
import roomio.sharedui.generated.resources.avatar_nova
import roomio.sharedui.generated.resources.avatar_orbit
import roomio.sharedui.generated.resources.avatar_prism
import roomio.sharedui.generated.resources.avatar_spark
import roomio.sharedui.generated.resources.avatar_sunny
import top.roomio.domain.profile.ProfileAvatarId
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import roomio.sharedui.generated.resources.*

internal enum class ProfileAvatar(
    val label: String,
    val resource: DrawableResource,
) {
    COMET("Comet", Res.drawable.avatar_comet),
    MINT("Mint", Res.drawable.avatar_mint),
    SUNNY("Sunny", Res.drawable.avatar_sunny),
    BERRY("Berry", Res.drawable.avatar_berry),
    CLOUD("Cloud", Res.drawable.avatar_cloud),
    EMBER("Ember", Res.drawable.avatar_ember),
    NOVA("Nova", Res.drawable.avatar_nova),
    ORBIT("Orbit", Res.drawable.avatar_orbit),
    PRISM("Prism", Res.drawable.avatar_prism),
    ECHO("Echo", Res.drawable.avatar_echo),
    SPARK("Spark", Res.drawable.avatar_spark),
    BLOOM("Bloom", Res.drawable.avatar_bloom),
}

internal fun ProfileAvatarId.toPresentationAvatar(): ProfileAvatar =
    ProfileAvatar.valueOf(name)

internal fun ProfileAvatar.toDomainAvatar(): ProfileAvatarId =
    ProfileAvatarId.valueOf(name)

internal val ProfileAvatar.localizedLabel: String
    @Composable get() = stringResource(when (this) {
        ProfileAvatar.COMET -> Res.string.avatar_label_comet
        ProfileAvatar.MINT -> Res.string.avatar_label_mint
        ProfileAvatar.SUNNY -> Res.string.avatar_label_sunny
        ProfileAvatar.BERRY -> Res.string.avatar_label_berry
        ProfileAvatar.CLOUD -> Res.string.avatar_label_cloud
        ProfileAvatar.EMBER -> Res.string.avatar_label_ember
        ProfileAvatar.NOVA -> Res.string.avatar_label_nova
        ProfileAvatar.ORBIT -> Res.string.avatar_label_orbit
        ProfileAvatar.PRISM -> Res.string.avatar_label_prism
        ProfileAvatar.ECHO -> Res.string.avatar_label_echo
        ProfileAvatar.SPARK -> Res.string.avatar_label_spark
        ProfileAvatar.BLOOM -> Res.string.avatar_label_bloom
    })
