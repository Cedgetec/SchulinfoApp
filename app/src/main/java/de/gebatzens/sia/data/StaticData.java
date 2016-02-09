/*
 * Copyright 2015 - 2016 Hauke Oldsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gebatzens.sia.data;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.fragment.RemoteDataFragment;

public class StaticData implements RemoteDataFragment.RemoteData {

    public byte[] data;
    public String name;
    public Throwable throwable;

    public void save() {
        try {
            OutputStream out = GGApp.GG_APP.openFileOutput("static_" + name, Context.MODE_PRIVATE);
            out.write(data);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean load() {
        try {
            InputStream in = GGApp.GG_APP.openFileInput("static_" + name);
            data = new byte[(int) new File("static_" + name).length()];
            in.read(data);
            in.close();

        } catch(IOException e) {
            Log.w("ggvp", "Failed to load static file: " + e.getMessage());
            throwable = e;
            return false;
        }

        return data.length > 0;
    }

    public File getFile() {
        return GGApp.GG_APP.getFileStreamPath("static_" + name);
    }

    public Throwable getThrowable() {
        return throwable;
    }

}
