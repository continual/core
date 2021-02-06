package io.continual.client.model;

import java.io.IOException;
import java.util.Set;

import io.continual.client.model.ModelClient.ModelServiceException;

public interface ModelReference
{
	/**
	 * Get the JSON data for an object
	 * @return JSON data
	 * @throws IOException 
	 * @throws ModelServiceException 
	 */
	String getData () throws ModelServiceException, IOException;

	/**
	 * Overwrite the data stored with the object
	 * @param jsonData
	 * @return this object
	 * @throws IOException 
	 * @throws ModelServiceException 
	 */
	ModelReference putData ( String jsonData ) throws ModelServiceException, IOException;
	
	/**
	 * Patch (merge) the data stored with the object
	 * @param jsonData
	 * @return this object
	 */
	ModelReference patchData ( String jsonData );

	/**
	 * Get relations for an object
	 * @return a set of relations 
	 */
	Set<ModelRelation> getRelations ();

	/**
	 * Get relations for an object
	 * @return a set of relations 
	 */
	Set<ModelRelation> getInboundRelations ();

	/**
	 * Get relations for an object
	 * @param named
	 * @return a set of relations 
	 */
	Set<ModelRelation> getInboundRelations ( String named );

	/**
	 * Get relations for an object
	 * @return a set of relations 
	 */
	Set<ModelRelation> getOutboundRelations ();

	/**
	 * Get relations for an object
	 * @param named
	 * @return a set of relations 
	 */
	Set<ModelRelation> getOutboundRelations ( String named );

	/**
	 * Relate this object to the given target object with the given name
	 * @param name
	 * @param to
	 * @return this object reference
	 */
	ModelReference relateTo ( String name, ModelObjectLocator to );

	/**
	 * Relate this object from the given source object with the given name
	 * @param name
	 * @param from
	 * @return this object reference
	 */
	ModelReference relateFrom ( String name, ModelObjectLocator from );
}
