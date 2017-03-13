package info.juanmendez.mockrealm.models;

import java.util.ArrayList;

import info.juanmendez.mockrealm.dependencies.Compare;
import info.juanmendez.mockrealm.decorators.RealmListDecorator;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.RealmQuery;
import io.realm.exceptions.RealmException;

import static org.powermock.api.mockito.PowerMockito.mock;

/**
 * Created by @juanmendezinfo on 2/19/2017.
 */
public class QueryNest {

    private ArrayList<Query> queries = new ArrayList<>();

    private boolean asAnd = true;
    private ArrayList<RealmList<RealmModel>> groupResults = new ArrayList<>();
    private int groupLevel = 0;
    private Class clazz;
    RealmQuery realmQuery;

    public QueryNest(Class clazz){
        this.clazz = clazz;
        realmQuery = mock(RealmQuery.class);
    }

    public void onTopGroupBegin(RealmList<RealmModel> realmList ){
        groupResults.add( realmList );
        onBeginGroupClause();
    }

    public RealmList<RealmModel> getQueryList(){

        if( !asAnd){
            return groupResults.get(groupLevel-1);
        }

        return groupResults.get(groupLevel);
    }

    public void setQueryList( RealmList<RealmModel> queryList ){
        if( asAnd ){
            groupResults.set( groupLevel, queryList );
        }else{

            RealmList<RealmModel> currentGroupList = groupResults.get( groupLevel );

            for (RealmModel realmModel: queryList) {
              if( !currentGroupList.contains( realmModel)){
                  currentGroupList.add( realmModel );
              }
            }
        }

        //if the las query is based on OR(), then bounce back to AND()
        this.asAnd = true;
    }

    public void onOrClause() {
        this.asAnd = false;
    }

    public void onBeginGroupClause(){

        RealmList<RealmModel> previousGroupList = groupResults.get( groupLevel );

        RealmList<RealmModel> nextGroupList = RealmListDecorator.create();
        nextGroupList.addAll( previousGroupList );

        groupLevel++;
        groupResults.add( nextGroupList );
    }

    public void onCloseGroupClause(){

        RealmList<RealmModel> currentGroupList = groupResults.get( groupLevel );
        groupResults.remove( groupLevel );

        groupLevel--;

        if( groupLevel < 0 ){
            throw( new RealmException("There is an attempt to close more than the number of groups created" ));
        }

        groupResults.get(groupLevel).clear();
        groupResults.get(groupLevel).addAll( currentGroupList );
    }

    public void onTopGroupClose(){
        onCloseGroupClause();

        if( groupLevel > 0 ){
            throw( new RealmException("Required to close all groups. Current group level is " + groupLevel ));
        }
    }

    public Class getClazz() {
        return clazz;
    }

    public void appendQuery( Query query ){
        queries.add( query );
    }

    public ArrayList<Query> getQueries(){
        return queries;
    }

    @Override
    public QueryNest clone(){
        return new QueryNest( this.clazz );
    }

    public Boolean isUsedByQuery( Query query ){

        Boolean used = false;

        switch ( query.getCondition() ){

            case Compare.startTopGroup:
                onTopGroupBegin( (RealmList<RealmModel>) query.getArgs()[0] );
                used = true;
                break;
            case Compare.startGroup:
                onBeginGroupClause();
                used = true;
                break;
            case Compare.or:
                onOrClause();
                used = true;
                break;
            case Compare.endGroup:
                onCloseGroupClause();
                used = true;
                break;
            case Compare.endTopGroup:
                onTopGroupClose();
                used = true;
                break;
        }

        return used;
    }

    public RealmQuery getRealmQuery() {
        return realmQuery;
    }
}