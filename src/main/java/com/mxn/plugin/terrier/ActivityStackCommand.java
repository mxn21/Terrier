/*
 * Copyright 2019 ChengzhiCao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mxn.plugin.terrier;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.ui.MessageType;
import org.jetbrains.android.util.AndroidOutputReceiver;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.util.*;

public class ActivityStackCommand {
    private static final List<DefaultMutableTreeNode> activityDumps = new ArrayList<>();
    private static final List<String> resumeActivities = new ArrayList<>();
    private static final Map<Integer, DefaultMutableTreeNode> nodes = new HashMap<>();
    private static DefaultMutableTreeNode currentNode;
    private static int currentDeep;
    private static boolean readOver = false;
    public static final String RESUME_TAG= "Resume Activity" ;

    public static List<DefaultMutableTreeNode> getActivityDumps2(IDevice device) {
        activityDumps.clear();
        resumeActivities.clear();
        nodes.clear();
        readOver = false ;
        try {
            device.executeShellCommand("dumpsys activity activities", new AndroidOutputReceiver() {
                @Override
                protected void processNewLine(@NotNull String line) {
                    if (!line.equals("") && !readOver) {
                        if (line.contains("mResumedActivity:")) {
                            String activityRecord = line.substring(line.indexOf("{")+1,line.indexOf("}"));
                            String activity = activityRecord.split(" ")[2] ;
                            resumeActivities.add(activity) ;
                        }
                        if (line.contains("Stack #") && line.contains(":")) {
                            String stack = line.split(":")[0] ;
                            createTree(0, stack) ;
                        }

                        if (line.contains("* Task") && line.contains("#")) {
                            String taskId = line.split("#")[1].split(" ")[0] ;
                            createTree(1, "task#"+ taskId) ;
                        }

                        if (line.contains("Activities=") && line.contains("[") && line.contains("]") ) {
                            String activities = line.substring(line.indexOf("[")+1,line.indexOf("]"));

                            if(activities.contains(",")) {
                                String[] contents = activities.split(",") ;
                                for (String content:contents) {
                                    String activityRecord = content.substring(content.indexOf("{")+1,content.indexOf("}"));
                                    String activity = activityRecord.split(" ")[2] ;
                                    createTree(2,  activity) ;
                                }
                            } else if (activities.contains("{")) {
                                String activityRecord =  activities.substring(activities.indexOf("{")+1,activities.indexOf("}"));
                                String activity = activityRecord.split(" ")[2] ;
                                createTree(2,  activity) ;
                            }
                        }
                        if (line.contains("ActivityStackSupervisor")) {
                            readOver = true ;
                        }
                    }
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }
            });
        } catch (TimeoutException | AdbCommandRejectedException | ShellCommandUnresponsiveException | IOException e) {
            e.printStackTrace();
        }
        // 删除空的stack
        activityDumps.removeIf(node -> node.getChildCount() == 0);
        // 加入resume Activity
        if (resumeActivities.size() > 0 ) {
            createTree(0,  RESUME_TAG) ;
            for(String act :resumeActivities) {
                createTree(1, act) ;
            }
        }
        return activityDumps;
    }

    private static void createTree(int deep, String content) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(content.trim());

        if (deep == 0 ) {
            activityDumps.add(node);
        } else  {
            if (deep > currentDeep) {
                currentNode.add(node);
            } else if (deep == currentDeep) {
                ((DefaultMutableTreeNode) currentNode.getParent()).add(node);
            } else {
                DefaultMutableTreeNode parent = getParentNode(deep);
                if (parent != null) {
                    parent.add(node);
                }
            }
        }
        currentNode = node ;
        currentDeep = deep ;
        nodes.put(deep, node);
    }



    private static DefaultMutableTreeNode getParentNode(int indent) {
        if (nodes.containsKey(indent)) {
            return (DefaultMutableTreeNode) nodes.get(indent).getParent();
        } else {
            while (--indent >= 0) {
                if (nodes.get(indent) != null)
                    return nodes.get(indent);
            }
        }
        return null;
    }

    public static void pushToMobile(IDevice device, String content) {
        try {
            device.executeShellCommand("input text " + content ,null) ;
        } catch (Exception e) {
            NotificationGroup notificationGroup = new NotificationGroup("terrier", NotificationDisplayType.BALLOON, false);
            Notification notification = notificationGroup.createNotification("push failed", MessageType.INFO);
            Notifications.Bus.notify(notification);
        }
    }
}
