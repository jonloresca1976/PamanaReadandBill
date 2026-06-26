package com.example.pamanareadandbill

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.filled.Home
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.pamanareadandbill.ui.theme.PamanaReadandBillTheme

@Composable
fun MainScreen(navController: NavController) {
    ReadandBillDashboard(navController)
}


@Composable
fun MenuItem(
    onItemClick: () -> Unit,
    @DrawableRes drawable: Int,
    @StringRes text: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onItemClick()
                }
        ) {
            Image(
                painter = painterResource(drawable),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
            )
            Text(
                text = stringResource(text),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun MainMenu(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    Column(modifier) {
        Spacer(Modifier.height(70.dp))
        LazyHorizontalGrid(
            rows = GridCells.Fixed(6),
            modifier = modifier
        ) {
            // Fix: Wrap MenuItem in item { } blocks as LazyGridScope requires item or items to emit composables.
            item {
                MenuItem(
                    drawable = R.drawable.mi_meter_reading,
                    text = R.string.mit_meter_reading,
                    modifier = Modifier.padding(8.dp),
                    onItemClick = {navController.navigate("read_entry")}
                )
            }
            item {
                MenuItem(
                    drawable = R.drawable.mi_upload_download,
                    text = R.string.mit_data,
                    modifier = Modifier.padding(8.dp),
                    onItemClick = {navController.navigate("data")}
                )
            }
            item {
                MenuItem(
                    drawable = R.drawable.mi_backup_restore,
                    text = R.string.mit_backup_restore,
                    modifier = Modifier.padding(8.dp),
                    onItemClick = {}
                )
            }
            item {
                MenuItem(
                    drawable = R.drawable.mi_reports,
                    text = R.string.mit_reports,
                    modifier = Modifier.padding(8.dp),
                    onItemClick = {}
                )
            }
            item {
                MenuItem(
                    drawable = R.drawable.mi_settings,
                    text = R.string.mit_settings,
                    modifier = Modifier.padding(8.dp),
                    onItemClick = {}
                )
            }
            item {
                MenuItem(
                    drawable = R.drawable.mi_printer,
                    text = R.string.mit_printer,
                    modifier = Modifier.padding(8.dp),
                    onItemClick = {}
                )
            }
        }
    }
}

@Composable
fun MenuGrid(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))
        MenuItem(
            drawable = R.drawable.mi_meter_reading,
            text = R.string.mit_meter_reading,
            modifier = Modifier.padding(8.dp),
            onItemClick = {navController.navigate("read_entry")}
        )

        MenuItem(
            drawable = R.drawable.mi_upload_download,
            text = R.string.mit_data,
            modifier = Modifier.padding(8.dp),
            onItemClick = {navController.navigate("data")}
        )

        MenuItem(
            drawable = R.drawable.mi_backup_restore,
            text = R.string.mit_backup_restore,
            modifier = Modifier.padding(8.dp),
            onItemClick = {}
        )

        MenuItem(
            drawable = R.drawable.mi_reports,
            text = R.string.mit_reports,
            modifier = Modifier.padding(8.dp),
            onItemClick = {}
        )

        MenuItem(
            drawable = R.drawable.mi_settings,
            text = R.string.mit_settings,
            modifier = Modifier.padding(8.dp),
            onItemClick = {navController.navigate("settings")}
        )

        MenuItem(
            drawable = R.drawable.mi_printer,
            text = R.string.mit_printer,
            modifier = Modifier.padding(8.dp),
            onItemClick = {}
        )
    }
}



@Composable
private fun BottomNavigation(modifier: Modifier = Modifier) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.height(60.dp)
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null
                )

            },
            label = {
                Text(
                    text = "Home"
                )
            },
            selected = true,
            onClick = {}
        )

    }
}

@Composable
fun TitleBar(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        Text (
            text = title,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
fun ReadandBillDashboard(navController: NavController) {
    PamanaReadandBillTheme {
        Scaffold(
            topBar = { TitleBar(stringResource(R.string.tb_dashboard)) },
            bottomBar = { BottomNavigation() },
        ) { padding ->
            Spacer(Modifier.height(30.dp))
            MenuGrid(
                modifier = Modifier.padding(8.dp).height(600.dp),
                navController
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MenuItemPreview() {
    PamanaReadandBillTheme {
        MenuItem(
            drawable = R.drawable.mi_printer,
            text = R.string.mit_printer,
            modifier = Modifier.padding(8.dp),
            onItemClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    PamanaReadandBillTheme {
        // Adding a height to the Preview as LazyHorizontalGrid needs a constrained height to render correctly.
        /*
        MainMenu(
            modifier = Modifier.padding(8.dp).height(600.dp)
        )*/
    }
}

@Preview(showBackground = true)
@Composable
fun MenuGridPreview() {
    val navController = rememberNavController()
    PamanaReadandBillTheme {
        MenuGrid(
            modifier = Modifier.padding(8.dp).height(600.dp),
            navController
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationPreview() {
    PamanaReadandBillTheme() {
        // Adding a height to the Preview as LazyHorizontalGrid needs a constrained height to render correctly.
        BottomNavigation(
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReadandBillDashboardPreview() {
    PamanaReadandBillTheme {
        //ReadandBillDashboard()
    }
}

