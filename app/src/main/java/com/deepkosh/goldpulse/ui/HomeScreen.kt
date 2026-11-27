package com.deepkosh.goldpulse.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepkosh.goldpulse.R
import com.deepkosh.goldpulse.config.GoldConfig
import com.deepkosh.goldpulse.data.GoldRates
import com.deepkosh.goldpulse.data.Karat
import com.deepkosh.goldpulse.data.KaratRate
import com.deepkosh.goldpulse.data.LiveQuote
import com.deepkosh.goldpulse.data.MarketQuote
import com.deepkosh.goldpulse.notifications.RateNotifier
import com.deepkosh.goldpulse.ui.theme.GoldCard
import com.deepkosh.goldpulse.ui.theme.GoldPulseTheme
import com.deepkosh.goldpulse.ui.theme.LocalPriceColors
import java.time.LocalTime
import kotlin.math.abs

@Composable
fun HomeRoute(viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var canNotify by remember { mutableStateOf(RateNotifier.canNotify(context)) }
    var permissionFromToggle by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        canNotify = RateNotifier.canNotify(context)
        viewModel.onNotificationPermissionResult(granted, permissionFromToggle)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            viewModel.takeFirstLaunchPermissionPrompt(RateNotifier.hasPermission(context))
        ) {
            permissionFromToggle = false
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LifecycleResumeEffect(Unit) {
        canNotify = RateNotifier.canNotify(context)
        viewModel.refreshIfStale()
        onPauseOrDispose { }
    }

    HomeScreen(
        state = state,
        notificationsBlocked = state.dailyNotificationEnabled && !canNotify,
        onRefresh = viewModel::refresh,
        onDailyNotificationChange = { enabled ->
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !RateNotifier.hasPermission(context)
            ) {
                permissionFromToggle = true
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setDailyNotification(enabled)
            }
        },
        onOpenNotificationSettings = { context.openNotificationSettings() },
        onMessageAction = { message ->
            when (message) {
                UserMessage.REFRESH_FAILED -> viewModel.refresh()
                UserMessage.NOTIFICATIONS_DENIED -> context.openNotificationSettings()
            }
        },
        onMessageShown = viewModel::messageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    notificationsBlocked: Boolean,
    onRefresh: () -> Unit,
    onDailyNotificationChange: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onMessageAction: (UserMessage) -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message
    val messageText = message?.let { stringResource(it.text) }
    val actionText = message?.let { stringResource(it.action) }
    LaunchedEffect(message) {
        if (message == null || messageText == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(messageText, actionText)
        if (result == SnackbarResult.ActionPerformed) onMessageAction(message)
        onMessageShown()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.isRefreshing) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = stringResource(R.string.action_refresh),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val rates = state.rates
        when {
            rates != null -> RatesContent(
                rates = rates,
                state = state,
                notificationsBlocked = notificationsBlocked,
                onRefresh = onRefresh,
                onDailyNotificationChange = onDailyNotificationChange,
                onOpenNotificationSettings = onOpenNotificationSettings,
                modifier = Modifier.padding(padding),
            )
            state.isRefreshing -> LoadingState(Modifier.padding(padding))
            else -> ErrorState(onRetry = onRefresh, modifier = Modifier.padding(padding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatesContent(
    rates: GoldRates,
    state: HomeUiState,
    notificationsBlocked: Boolean,
    onRefresh: () -> Unit,
    onDailyNotificationChange: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { HeroCard(rates) }
            if (state.showingSavedRate) {
                item { SavedRateBanner() }
            }
            item { KaratTableCard(rates) }
            item { DisclaimerCard() }
            item {
                NotificationCard(
                    enabled = state.dailyNotificationEnabled,
                    blocked = notificationsBlocked,
                    onEnabledChange = onDailyNotificationChange,
                    onOpenSettings = onOpenNotificationSettings,
                )
            }
            item { CalculationCard(rates) }
            item {
                Text(
                    text = stringResource(R.string.data_sources),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroCard(rates: GoldRates) {
    val rate = rates.rate(Karat.K24)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(GoldCard.background)
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.hero_label),
            style = MaterialTheme.typography.labelLarge,
            color = GoldCard.content.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = RateFormat.rupees(rate.perTenGrams),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = GoldCard.content,
        )
        Text(
            text = stringResource(R.string.per_ten_grams),
            style = MaterialTheme.typography.bodyMedium,
            color = GoldCard.content,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.per_gram_value, RateFormat.rupees(rate.perGram)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = GoldCard.content,
        )
        Spacer(Modifier.height(16.dp))
        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.55f)) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val change = rate.changePerTenGrams
                val percent = rate.changePercent
                if (change != null && percent != null) {
                    val trend = RateFormat.trend(change)
                    Text(
                        text = changeText(trend, change, percent),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (trend) {
                            RateFormat.Trend.UP -> GoldCard.up
                            RateFormat.Trend.DOWN -> GoldCard.down
                            RateFormat.Trend.FLAT -> GoldCard.content
                        },
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.change_vs_yesterday),
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldCard.content,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.change_unavailable),
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldCard.content,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.last_updated, RateFormat.dateTime(rates.live.fetchedAtMillis)),
            style = MaterialTheme.typography.bodySmall,
            color = GoldCard.content.copy(alpha = 0.85f),
        )
        Text(
            text = stringResource(R.string.hero_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = GoldCard.content.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun SavedRateBanner() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_cloud_off), contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.saved_rate_banner), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun KaratTableCard(rates: GoldRates) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.rates_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                val header = MaterialTheme.typography.labelMedium
                val headerColor = MaterialTheme.colorScheme.onSurfaceVariant
                Text(stringResource(R.string.column_karat), Modifier.weight(1f), headerColor, style = header)
                Text(
                    stringResource(R.string.column_one_gram), Modifier.weight(1f), headerColor,
                    style = header, textAlign = TextAlign.End,
                )
                Text(
                    stringResource(R.string.column_ten_grams), Modifier.weight(1.3f), headerColor,
                    style = header, textAlign = TextAlign.End,
                )
            }
            rates.karats.forEach { rate ->
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                KaratRow(rate)
            }
            Text(
                text = stringResource(R.string.rates_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun KaratRow(rate: KaratRate) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(rate.karat.labelRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.purity, RateFormat.percent(rate.karat.purity, maxDecimals = 1, minDecimals = 0)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = RateFormat.rupees(rate.perGram),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
            Text(
                text = RateFormat.rupees(rate.perTenGrams),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            ChangeLabel(rate, MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ChangeLabel(rate: KaratRate, style: TextStyle) {
    val change = rate.changePerTenGrams
    val percent = rate.changePercent
    if (change == null || percent == null) {
        Text(stringResource(R.string.change_unknown), style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val trend = RateFormat.trend(change)
    val colors = LocalPriceColors.current
    Text(
        text = changeText(trend, change, percent),
        style = style,
        fontWeight = FontWeight.Medium,
        color = when (trend) {
            RateFormat.Trend.UP -> colors.up
            RateFormat.Trend.DOWN -> colors.down
            RateFormat.Trend.FLAT -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun changeText(trend: RateFormat.Trend, change: Double, percent: Double): String =
    stringResource(
        RateFormat.changeTemplate(trend),
        RateFormat.rupees(abs(change)),
        RateFormat.percent(abs(percent) / 100),
    )

@Composable
private fun DisclaimerCard() {
    InfoCard(
        icon = R.drawable.ic_info,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = stringResource(R.string.disclaimer_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.disclaimer_body), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NotificationCard(
    enabled: Boolean,
    blocked: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    InfoCard(
        icon = R.drawable.ic_notifications,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notification_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        R.string.notification_card_subtitle,
                        RateFormat.time(LocalTime.of(GoldConfig.NOTIFICATION_HOUR, GoldConfig.NOTIFICATION_MINUTE)),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        if (blocked) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.notification_blocked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onOpenSettings, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.action_open_settings))
            }
        }
    }
}

@Composable
private fun CalculationCard(rates: GoldRates) {
    val market = rates.live.market
    InfoCard(
        icon = R.drawable.ic_calculate,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Text(
            text = stringResource(R.string.calc_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        CalcRow(R.string.calc_spot, stringResource(R.string.calc_spot_value, RateFormat.usd(market.spotUsdPerOunce)))
        CalcRow(R.string.calc_fx, RateFormat.rupeesWithPaise(market.usdInr))
        CalcRow(
            R.string.calc_duty,
            RateFormat.percent(GoldConfig.IMPORT_DUTY_PERCENT / 100, maxDecimals = 2, minDecimals = 0),
        )
        CalcRow(R.string.calc_source, rates.live.source)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.calc_formula,
                GoldConfig.GRAMS_PER_TROY_OUNCE.toString(),
                GoldConfig.PURITY_22K.toString(),
                GoldConfig.PURITY_18K.toString(),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CalcRow(@StringRes label: Int, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoCard(
    @DrawableRes icon: Int,
    containerColor: Color,
    contentColor: Color,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(Modifier.padding(20.dp)) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.padding(top = 2.dp).size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { content() }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_cloud_off),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.error_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.error_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

private val Karat.labelRes: Int
    @StringRes get() = when (this) {
        Karat.K24 -> R.string.karat_24
        Karat.K22 -> R.string.karat_22
        Karat.K18 -> R.string.karat_18
    }

private fun Context.openNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No settings screen available on this device.
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun HomeScreenPreview() {
    val rates = GoldRates(
        live = LiveQuote(MarketQuote(3640.80, 92.52), "gold-api.com", System.currentTimeMillis()),
        yesterday = MarketQuote(3600.00, 92.40),
    )
    GoldPulseTheme {
        HomeScreen(
            state = HomeUiState(rates = rates, isRefreshing = false, dailyNotificationEnabled = true),
            notificationsBlocked = false,
            onRefresh = {},
            onDailyNotificationChange = {},
            onOpenNotificationSettings = {},
            onMessageAction = {},
            onMessageShown = {},
        )
    }
}
