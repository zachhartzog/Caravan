package com.caravan.caravan;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.mobile.auth.core.IdentityManager;
import com.caravan.caravan.DynamoCacheDB.DynamoCacheDAO;
import com.caravan.caravan.DynamoCacheDB.DynamoCacheDatabase;
import com.caravan.caravan.DynamoCacheDB.Entity.BlueprintLocation;
import com.caravan.caravan.DynamoCacheDB.Entity.Location;
import com.caravan.caravan.DynamoCacheDB.Entity.UserBlueprint;
import com.caravan.caravan.DynamoCacheDB.Entity.UserBlueprintLocationPairing;
import com.caravan.caravan.DynamoDB.CuratedDO;
import com.caravan.caravan.DynamoDB.DatabaseAccess;
import com.caravan.caravan.DynamoDB.UserDO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by meaghan on 4/22/18.
 */

public class saveLocDialog extends DialogFragment {
    private DatabaseAccess m_db = DatabaseAccess.getInstance(getActivity());
    private String m_loc;
    static saveLocDialog newInstance(String loc) {
        saveLocDialog d= new saveLocDialog();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("loc", loc);
        d.setArguments(args);

        return d;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        m_loc = getArguments().getString("loc");
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String[] items = getItems();
        final IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();
        boolean isSignedIn = false;
        try {
            isSignedIn = identityManager.isUserSignedIn();
        } catch (NullPointerException e) {
            isSignedIn = false;
        }
        if (isSignedIn)  {
            builder.setTitle("Choose where to save to:")
                    .setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // The 'which' argument contains the index position
                            // of the selected item'
                            if (which == 0) {
                                try {
                                    CuratedDO loc = (CuratedDO) m_db.getItem(m_loc, "location", "curated");
                                    m_db.userSaveLocation(loc);
                                    cacheLocation(m_loc);
                                } catch (ExecutionException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else if (which == 1) {
                                FragmentTransaction ft = getFragmentManager().beginTransaction();
                                Fragment prev = getFragmentManager().findFragmentByTag("dialog");
                                if (prev != null) {
                                    ft.remove(prev);
                                }
                                ft.addToBackStack(null);

                                // Create and show the dialog.
                                createGuideDialog create = createGuideDialog.newInstance(m_loc);
                                create.show(ft, "dialog");

                            } else {
                                try {
                                    UserDO bp = (UserDO) m_db.getItem(items[which], "blueprint", "user");
                                    m_db.addLocationToBlueprint(bp, m_loc);
                                    cacheLocationToUserBlueprint(m_loc, items[which]);
                                } catch (ExecutionException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
        }
        else {
            builder.setMessage("Please log in to continue")
                    .setTitle("Error Saving Location");
            builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.show();
        }

        // Create the AlertDialog object and return it
        return builder.create();
    }


    private String[] getItems() {
        String[] items = new String[2];
        items[0] = "Save to Library";
        items[1] = "Create New Blueprint";
        List<UserDO> userBP = m_db.getAllUserBlueprints();
        if (!userBP.isEmpty())  {
            items = new String[userBP.size() + 2];
            items[0] = "Save to Library";
            items[1] = "Create New Blueprint";
            for (int i = 0; i < userBP.size(); i++) {
                items[i+2] = userBP.get(i).getName();
            }
        }
        return items;
    }
    //Used for saving a location to a (User) blueprint
    private void cacheLocationToUserBlueprint(String locationName, String blueprintName) {
        DynamoCacheDatabase database = DynamoCacheDatabase.getInstance(getActivity());
        DynamoCacheDAO dao = database.dynamoCacheDAO();
        DatabaseAccess task = DatabaseAccess.getInstance(getActivity());
        Future<CuratedDO> loc = task.getCuratedItem("location", locationName);
        Future<UserDO> blu = task.getUserItem(blueprintName);
        CuratedDO location = new CuratedDO();
        UserDO blueprint = new UserDO();
        try {
            location = loc.get();
            blueprint = blu.get();
        } catch (ExecutionException | InterruptedException e) {
        }
        if (dao.getUserBlueprintById(blueprint.getName()) == null)
            dao.insertUserBlueprint(new UserBlueprint(blueprint.getName(),blueprint));
        if (dao.getBlueprintLocationById(location.getName()) == null)
            dao.insertBlueprintLocation(new BlueprintLocation(location.getName(),location));
        dao.insertUserBlueprintLocationPairing(new UserBlueprintLocationPairing(blueprint.getName(), location.getName()));
        for(CuratedDO blueprintLocation: task.getBlueprintLocations(blueprint)) {
            if(dao.getBlueprintLocationById(blueprintLocation.getName()) == null)
                dao.insertBlueprintLocation(new BlueprintLocation(blueprintLocation.getName(), blueprintLocation));
            dao.insertUserBlueprintLocationPairing(new UserBlueprintLocationPairing(blueprint.getName(), blueprintLocation.getName()));
        }

        String logTag = "SAVE_LOC_DIALOG_CACHE";
        Log.d(logTag, "Locations:");
        for(BlueprintLocation location1: dao.getAllBlueprintLocations())
            Log.d(logTag, location1.getId());
        Log.d(logTag, "Blueprints:");
        for(UserBlueprint bp: dao.getAllUserBlueprints())
            Log.d(logTag,bp.getId());
        Log.d(logTag,"Pairings:");
        for(UserBlueprintLocationPairing pairing: dao.getAllUserBlueprintLocationPairings())
            Log.d(logTag,"BlueprintId: "+pairing.getBlueprint_id()+" LocationId: " + pairing.getLocation_id());
    }

    //Used for saving a location to your account.
    private void cacheLocation(String locationName) {
        DatabaseAccess task = DatabaseAccess.getInstance(getActivity());
        Future<CuratedDO> item = task.getCuratedItem("location", locationName);
        CuratedDO location = new CuratedDO();
        try {
            location = item.get();
        } catch (ExecutionException | InterruptedException e) {

        }
        DynamoCacheDatabase database = DynamoCacheDatabase.getInstance(getActivity());
        final DynamoCacheDAO dao = database.dynamoCacheDAO();
        if (dao.getLocationById(location.getName()) == null) {
            Location cachedLocation= new Location(locationName, location);
            dao.insertLocation(cachedLocation);
        }
    }
}



