/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.kiny.bluetooth;

/**
 * Defines several constants used between {@link BluetoothClient} and the UI.
 */
public interface Constants {
    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_CONNECTED = 1;
    int MESSAGE_CONNECTION_LOST = 2;
    int MESSAGE_INCOMING_MESSAGE = 3;
}
