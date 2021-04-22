/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package io.continual.iam;

import java.io.Closeable;

import io.continual.iam.access.AccessManager;
import io.continual.iam.access.AclUpdateListener;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.IdentityManager;
import io.continual.iam.tags.TagManager;
import io.continual.metrics.MetricsSupplier;

/**
 * An IAM DB implements all facets of identity and access management.
 *
 * @param <I>
 * @param <G>
 */
public interface IamDb<I extends Identity,G extends Group>
	extends IdentityManager<I>, AccessManager<G>, TagManager, AclUpdateListener, MetricsSupplier, Closeable
{
	default void start () throws IamSvcException
	{
	}

	default void close ()
	{
	}
}
