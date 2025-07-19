import React from "react";
import { Outlet } from "react-router-dom";
import {
  Box,
  Drawer,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemButton,
  Divider,
} from "@mui/material";
import {
  Menu as MenuIcon,
  Forum as ForumIcon,
  AccountTree as AccountTreeIcon,
  Analytics as AnalyticsIcon,
  Settings as SettingsIcon,
  Logout as LogoutIcon,
  Business as BusinessIcon,
  AdminPanelSettings as AdminIcon,
} from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import { useAppSelector, useAppDispatch } from "../store";
import { toggleSidebar } from "../store/slices/uiSlice";
import { logout } from "../store/slices/authSlice";
import OrganizationSwitcher from "./OrganizationSwitcher";

const drawerWidth = 240;

const Layout: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { sidebarOpen } = useAppSelector((state) => state.ui);
  const { user } = useAppSelector((state) => state.auth);

  const handleLogout = () => {
    dispatch(logout());
    navigate("/login");
  };

  // Check if user is admin
  const isAdmin = user?.role === "admin" || user?.role === "superadmin";

  const menuItems = [
    { text: "Debates", icon: <ForumIcon />, path: "/debates" },
    { text: "Workflow Editor", icon: <AccountTreeIcon />, path: "/workflow-editor" },
    { text: "Analytics", icon: <AnalyticsIcon />, path: "/analytics" },
    { text: "Settings", icon: <SettingsIcon />, path: "/settings" },
  ];

  const adminMenuItems = [
    { text: "Organization Management", icon: <BusinessIcon />, path: "/organization-management" },
  ];

  return (
    <Box sx={{ display: "flex" }}>
      <AppBar
        position="fixed"
        sx={{
          width: `calc(100% - ${sidebarOpen ? drawerWidth : 0}px)`,
          ml: `${sidebarOpen ? drawerWidth : 0}px`,
          transition: "width 0.3s, margin 0.3s",
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            onClick={() => dispatch(toggleSidebar())}
            edge="start"
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            Zamaz Debate System
          </Typography>
          {user && (
            <Box sx={{ display: "flex", alignItems: "center", mr: 2 }}>
              {isAdmin && (
                <AdminIcon sx={{ mr: 1, color: "gold" }} />
              )}
              <Typography variant="body2">
                {user.username}
                {isAdmin && (
                  <Typography 
                    component="span" 
                    variant="caption" 
                    sx={{ 
                      ml: 1, 
                      px: 1, 
                      py: 0.5, 
                      bgcolor: "rgba(255,215,0,0.2)", 
                      borderRadius: 1,
                      color: "gold"
                    }}
                  >
                    ADMIN
                  </Typography>
                )}
              </Typography>
            </Box>
          )}
        </Toolbar>
      </AppBar>
      <Drawer
        sx={{
          width: drawerWidth,
          flexShrink: 0,
          "& .MuiDrawer-paper": {
            width: drawerWidth,
            boxSizing: "border-box",
          },
        }}
        variant="persistent"
        anchor="left"
        open={sidebarOpen}
      >
        <Toolbar />
        <Box sx={{ overflow: "auto" }}>
          <Box sx={{ p: 2 }}>
            <OrganizationSwitcher />
          </Box>
          <Divider />
          <List>
            {menuItems.map((item) => (
              <ListItem key={item.text} disablePadding>
                <ListItemButton onClick={() => navigate(item.path)}>
                  <ListItemIcon>{item.icon}</ListItemIcon>
                  <ListItemText primary={item.text} />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
          
          {isAdmin && (
            <>
              <Divider />
              <List>
                <ListItem>
                  <ListItemIcon>
                    <AdminIcon />
                  </ListItemIcon>
                  <ListItemText 
                    primary="Administration" 
                    primaryTypographyProps={{ 
                      variant: "subtitle2", 
                      color: "primary",
                      fontWeight: "bold"
                    }} 
                  />
                </ListItem>
                {adminMenuItems.map((item) => (
                  <ListItem key={item.text} disablePadding>
                    <ListItemButton onClick={() => navigate(item.path)}>
                      <ListItemIcon>{item.icon}</ListItemIcon>
                      <ListItemText primary={item.text} />
                    </ListItemButton>
                  </ListItem>
                ))}
              </List>
            </>
          )}
          <Divider />
          <List>
            <ListItem disablePadding>
              <ListItemButton onClick={handleLogout}>
                <ListItemIcon>
                  <LogoutIcon />
                </ListItemIcon>
                <ListItemText primary="Logout" />
              </ListItemButton>
            </ListItem>
          </List>
        </Box>
      </Drawer>
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          bgcolor: "background.default",
          p: 3,
          transition: "margin 0.3s",
          marginLeft: sidebarOpen ? `${drawerWidth}px` : 0,
          minHeight: "100vh",
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
};

export default Layout;
